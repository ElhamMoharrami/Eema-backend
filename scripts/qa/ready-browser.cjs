#!/usr/bin/env node
// Reuses AUTH/POL/TRUST Chrome CDP + ws. Dedicated fictional venue fixtures only.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
if(process.argv[2]!=='--local-qa')throw Error('Use --local-qa');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'docs/qa/evidence/readiness'),run='QA-READY-'+Date.now(),password='QaFixture-2026!',checks=[],secrets=[];
fs.mkdirSync(out,{recursive:true});
const compose=['compose','-p','eema-ready-qa','-f',path.join(root,'scripts/qa/ready-compose.yml')];
function sql(query){return cp.execFileSync('docker',[...compose,'exec','-T','postgres','psql','-X','-U','eema','-d','eema_ready_qa','-At','-v','ON_ERROR_STOP=1'],{input:query,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();}
function record(name,details={}){checks.push({name,result:'PASS',...details});fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'RUNNING',run,checks},null,2));}
async function api(route,method,body,account,expected=200){const r=await fetch('http://localhost:8080/api'+route,{signal:AbortSignal.timeout(20000),method,headers:{...(body?{'Content-Type':'application/json'}:{}),...(account?{Authorization:'Bearer '+account.accessToken}:{})},...(body?{body:JSON.stringify(body)}:{})});assert.equal(r.status,expected,route);return r.status===204?null:r.json();}
const booleanLabels={naturalLight:'Natural light',blackoutPossible:'Blackout possible',loadingAccess:'Loading access',parkingAvailable:'Parking available',liftAvailable:'Lift available',wheelchairAccessible:'Wheelchair accessible',bathroomsAvailable:'Bathrooms available',soundRecordingSuitable:'Sound recording suitable'};
async function connect(tab){const ws=new WS(tab.webSocketDebuggerUrl);await new Promise(r=>ws.on('open',r));let seq=0;const pending=new Map(),methods=new Map(),events=[],paused=[],requests=[];let intercept=null;
 ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error('CDP request failed')):p.resolve(m.result);}else if(m.method==='Network.requestWillBeSent'){methods.set(m.params.requestId,m.params.request.method);if(m.params.request.method==='PUT'&&m.params.request.url.includes('/api/venues/'))requests.push(JSON.parse(m.params.request.postData||'{}'));}else if(m.method==='Network.responseReceived'){let r=m.params.response;events.push({path:new URL(r.url).pathname,method:methods.get(m.params.requestId),status:r.status});}else if(m.method==='Fetch.requestPaused'){if(intercept)intercept(m.params);else paused.push(m.params.requestId);}});
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error('Browser evaluation failed: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text));return r.result.value;};
 async function wait(expression,label){for(let i=0;i<150;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100));}throw Error('Browser timeout: '+label);}
 async function go(route){const target='http://localhost:3000'+route;let result;for(let attempt=0;attempt<3;attempt++){result=await send('Page.navigate',{url:target});if(result.errorText!=='net::ERR_ABORTED')break;await new Promise(r=>setTimeout(r,250));}if(result.errorText)throw Error('Navigation failed: '+result.errorText);await wait('location.href==='+JSON.stringify(target)+' && document.readyState==="complete"','document ready');}
 async function click(text){await wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.trim()==='+JSON.stringify(text)+'&&!b.disabled)','button '+text);await evaluate(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!b)throw Error('button missing');b.click();})()`);}
 async function fill(values){await evaluate(`(()=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;document.querySelectorAll('form input').forEach((el,i)=>{set.call(el,${JSON.stringify(values)}[i]);el.dispatchEvent(new Event('input',{bubbles:true}));});})()`);}
 const session=async()=>{const s=await evaluate("JSON.parse(localStorage.getItem('eema:auth'))");if(s)secrets.push(s.accessToken,s.refreshToken);return s;};
 const screen=async name=>{const metrics=await send('Page.getLayoutMetrics');const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip:{x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1}});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,requests,ws,setIntercept:fn=>{intercept=fn}};
}

(async()=>{
 assert.equal(sql('select current_database()'),'eema_ready_qa');
 const accounts={};
 for(const role of ['OWNER','ADMIN','SCOUT']){
   const a=await api('/auth/register','POST',{email:run+'-'+role+'@example.test',fullName:'QA Venue '+role,password,role:role==='ADMIN'?'SCOUT':role},null,201);
   assert.equal(sql(`select count(*) from users where id='${a.user.id}';`),'1');
   sql(`update users set email_verified=true${role==='ADMIN'?",role='ADMIN'":''} where id='${a.user.id}';`);
   accounts[role]=await api('/auth/login','POST',{email:a.user.email,password},null);
 }
 const c=await connect((await(await fetch('http://localhost:9223/json')).json())[0]);
 async function login(role,route){await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(accounts[role]))+')');await c.go(route);}
 async function field(label,value){
   const expression='Array.from(document.querySelectorAll("label")).find(l=>l.childNodes[0]?.textContent.trim()==='+JSON.stringify(label)+')?.querySelector("input,textarea,select")';
   await c.wait('!!('+expression+')','field '+label);
   await c.evaluate('(()=>{const e='+expression+';const proto=e.tagName==="SELECT"?HTMLSelectElement.prototype:e.tagName==="TEXTAREA"?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;Object.getOwnPropertyDescriptor(proto,"value").set.call(e,'+JSON.stringify(value)+');e.dispatchEvent(new Event(e.tagName==="SELECT"?"change":"input",{bubbles:true}));})()');
 }
 async function textarea(selector,value){await c.wait('!!document.querySelector('+JSON.stringify(selector)+')','textarea');await c.evaluate('(()=>{const e=document.querySelector('+JSON.stringify(selector)+');Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,"value").set.call(e,'+JSON.stringify(value)+');e.dispatchEvent(new Event("input",{bubbles:true}));})()');}
 async function saved(){await c.wait("document.body?.textContent.includes('Listing details saved.')",'saved listing');}
 await c.go('/');await c.evaluate("localStorage.removeItem('eema:auth')");await c.go('/');await c.click('Sign in / register');await c.wait("!!document.querySelector('form input')",'login');await c.fill([accounts.OWNER.user.email,password]);await c.evaluate("document.querySelector('form button[type=submit]').click()");await c.wait("document.querySelector('header')?.textContent.includes('QA Venue OWNER')",'owner login');
 accounts.OWNER=await c.session();await c.go('/owner/venues/new');
 for(const [label,value] of Object.entries({'Venue name':run,'Neighbourhood':'QA District','Size (m²)':'100','Hourly rate':'100','Minimum booking':'60','Minimum lead time':'60'}))await field(label,value);
 await c.click('Create listing');await c.wait("location.pathname.startsWith('/owner/venues/') && location.pathname.split('/').pop().length===36",'created route');
 const id=await c.evaluate("location.pathname.split('/').pop()"),ownerRoute='/owner/venues/'+id,adminRoute='/admin/venues/'+id;
 await c.wait("document.body?.textContent.includes('Edit details')",'owner editor');let venue=await api('/venues/'+id,'GET',null,accounts.OWNER);
 assert.equal(venue.status,'DRAFT');assert.equal(venue.timezone,'Europe/Amsterdam');assert.deepEqual(venue.photoPaths,[]);
 const verification=await api('/owner/verification/submit','POST',{note:'QA fictional owner review'},accounts.OWNER);await api('/admin/owner-verifications/'+verification.id+'/approve','POST',{verificationMethod:'MANUAL_REVIEW'},accounts.ADMIN);
 const ids=[];
 const types=fs.readFileSync(path.join(root,'src/main/java/com/eema/backend/entity/enums/PolicyType.java'),'utf8').match(/\b[A-Z][A-Z_]+\b/g).filter(x=>x!=='UUID');
 for(const policyType of [...new Set(types)]){
   const p=await api('/admin/policies','POST',{policyType,version:run+'-'+policyType,title:'QA TEST CONTENT — NOT LEGAL TERMS',content:'QA TEST CONTENT — NOT LEGAL TERMS',effectiveAt:'2099-01-01T00:00:00Z'},accounts.ADMIN,201);
   await api('/admin/policies/'+p.id+'/publish','POST',null,accounts.ADMIN);ids.push(p.id);
 }
 await api('/policies/accept','POST',{policyVersionIds:ids},accounts.OWNER);

 const labels={maxCrewSize:'Crew capacity',permittedActivities:'Permitted production activities',productionRules:'Production rules',naturalLight:'Natural-light information',loadingAccess:'Loading-access information',bathroomsAvailable:'Bathroom information',soundRecordingSuitable:'Sound-recording suitability',hourlyRateEur:'Pricing',minimumBookingMinutes:'Minimum booking duration',minimumLeadTimeMinutes:'Minimum lead time',timezone:'Timezone',photoPaths:'Listing photos'};
 const ownerIssues="Array.from(document.querySelectorAll('section')).find(s=>s.textContent.includes('Listing moderation'))?.querySelector('ul')";
 await login('OWNER',ownerRoute);await c.wait("document.body?.textContent.includes('Listing items to complete')",'readiness list');
 let issues=(await api('/venues/'+id+'/moderation','GET',null,accounts.OWNER)).readinessIssues;
 assert.equal(issues.length,8);assert.deepEqual(await c.evaluate('Array.from(('+ownerIssues+')?.querySelectorAll("li")||[]).map(e=>e.textContent)'),issues.map(k=>labels[k]));
 await c.screen('owner-multiple-missing');record('Spring returns all eight API-reachable missing conditions; owner displays complete readable list');
 await c.click('Submit for review');
 // Wait for authoritative GET following the actual POST conflict, not a timing sleep.
 for(let i=0;i<150&&!c.events.some(e=>e.path.endsWith('/submit-for-review')&&e.status===409);i++)await new Promise(r=>setTimeout(r,100));
 assert(c.events.some(e=>e.path.endsWith('/submit-for-review')&&e.status===409));
 await c.wait("!document.body?.textContent.includes('Loading moderation status') && !Array.from(document.querySelectorAll('button')).some(b=>b.textContent==='Submitting…')",'conflict refetch');
 const ownerError=await c.evaluate("Array.from(document.querySelectorAll('[role=alert]')).some(e=>e.textContent.includes('operation or record state changed'))");
 assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'DRAFT');
 await c.screen(process.argv.includes('--probe')?'before-owner-conflict':'owner-conflict');
 if(!process.argv.includes('--probe'))assert.equal(ownerError,true);
 record('Incomplete submission returns 409, remains DRAFT and refreshes checklist',{errorVisible:ownerError});
 for(const label of ['Natural light','Loading access','Bathrooms available','Sound recording suitable'])await field(label,'false');
 await field('Maximum crew size','20');await field('Rules and considerations','QA fictional readiness rules');
 await c.evaluate("(()=>{const f=Array.from(document.querySelectorAll('fieldset')).find(f=>f.querySelector('legend')?.textContent==='Permitted production activities');const e=f.querySelector('select');Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(e,'specified');e.dispatchEvent(new Event('change',{bubbles:true}));})()");
 await c.wait("Array.from(document.querySelectorAll('label')).some(l=>l.textContent.trim()==='Film'&&l.querySelector('input'))",'activities');await c.evaluate("Array.from(document.querySelectorAll('label')).find(l=>l.textContent.trim()==='Film').querySelector('input').click()");
 await c.click('Save listing details');await saved();
 const imageFile='/tmp/eema-ready-qa.png';fs.writeFileSync(imageFile,Buffer.from('iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAGUlEQVR4nGPUcNvHQApgIkn1qIZRDUNKAwDIDwFMvejdtwAAAABJRU5ErkJggg==','base64'));
 async function upload(){const document=await c.send('DOM.getDocument');const node=await c.send('DOM.querySelector',{nodeId:document.root.nodeId,selector:'input[type=file]'});await c.send('DOM.setFileInputFiles',{nodeId:node.nodeId,files:[imageFile]});await c.wait("document.body?.textContent.includes('Photo uploaded and attached')",'photo upload');}
 await upload();venue=await api('/venues/'+id,'GET',null,accounts.OWNER);assert.equal(venue.photoPaths.length,1);record('Production activity native persistence and actual photo upload');

 await c.go(ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'reload');
 assert.deepEqual((await api('/venues/'+id+'/moderation','GET',null,accounts.OWNER)).readinessIssues,[]);
 await c.wait("!document.body?.textContent.includes('Listing items to complete')",'issues cleared');
 for(const label of ['Natural light','Loading access','Bathrooms available','Sound recording suitable'])assert.equal(await c.evaluate('Array.from(document.querySelectorAll("label")).find(l=>l.childNodes[0]?.textContent.trim()==='+JSON.stringify(label)+').querySelector("select").value'),'false');
 await c.screen('owner-complete-false');record('All four required Boolean No answers are complete; fixing fields and uploading a photo clears Spring checklist');
 await c.click('Submit for review');await c.wait("document.body?.textContent.includes('Listing under review')",'pending');record('Complete venue submits successfully and refetches PENDING');
 await login('ADMIN',adminRoute);await c.click('Approve listing');await c.wait("!!document.querySelector('#note')",'approval form');
 venue=await api('/venues/'+id,'GET',null,accounts.OWNER);await api('/venues/'+id,'PUT',{...venue,productionRules:null},accounts.OWNER);
 await c.click('Confirm');await c.wait("document.body?.textContent.includes('Backend readiness issues')",'admin incomplete refetch');
 assert(c.events.some(e=>e.path.endsWith('/approve')&&e.status===409));
 const adminLabels=await c.evaluate("Array.from(document.querySelectorAll('div')).find(e=>e.className==='label'&&e.textContent==='Backend readiness issues')?.parentElement.querySelector('ul')?.textContent");
 await c.screen(process.argv.includes('--probe')?'before-admin-readiness':'admin-readiness-conflict');
 if(process.argv.includes('--probe')){
  fs.writeFileSync(path.join(out,'ui-before.json'),JSON.stringify({result:'REPRODUCED',ownerConflictErrorVisible:ownerError,adminReadinessText:adminLabels,expectedAdminLabel:'Production rules'},null,2)+'\n');
  await c.evaluate("localStorage.removeItem('eema:auth')");await c.send('Browser.close');return;
 }
 assert.equal(adminLabels,'Production rules');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');record('Stale admin approval returns 409; refetched PENDING shows readable current readiness issue');
 await login('OWNER',ownerRoute);await field('Rules and considerations','QA restored readiness rules');await c.click('Save listing details');await saved();
 await c.wait("!document.body?.textContent.includes('Listing items to complete')",'restored pending');
 await login('ADMIN',adminRoute);await c.click('Approve listing');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Live')&&!document.querySelector('#note')",'approved');
 assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'ACTIVE');record('Restore missing field through owner editor; admin approval succeeds');
 await c.screen('admin-approved');await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');
 fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'PASS',run,checks},null,2)+'\n');
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',run,checks,error:e.message},null,2));console.error(e.message);process.exit(1)});
