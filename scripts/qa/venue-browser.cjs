#!/usr/bin/env node
// Reuses AUTH/POL/TRUST Chrome CDP + ws. Dedicated fictional venue fixtures only.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
if(process.argv[2]!=='--local-qa')throw Error('Use --local-qa');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'docs/qa/evidence/venue'),run='QA-VEN-'+Date.now(),password='QaFixture-2026!',checks=[],secrets=[];
fs.mkdirSync(out,{recursive:true});
const compose=['compose','-p','eema-venue-qa','-f',path.join(root,'scripts/qa/venue-compose.yml')];
function sql(query){return cp.execFileSync('docker',[...compose,'exec','-T','postgres','psql','-X','-U','eema','-d','eema_venue_qa','-At','-v','ON_ERROR_STOP=1'],{input:query,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();}
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
 assert.equal(sql('select current_database()'),'eema_venue_qa');
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
 assert.equal(sql(`select count(*) from policy_acceptances where user_id='${accounts.OWNER.user.id}';`),'0');assert.equal(sql(`select count(*) from owner_verifications where owner_id='${accounts.OWNER.user.id}';`),'0');
 await c.screen('owner-created-draft');record('OWNER real login and UI draft creation without owner verification or policy acceptance');
 // Every tri-state control traverses actual request, PostgreSQL and reload; no truthiness conversion.
 for(const value of [null,false,true]){
   for(const label of Object.values(booleanLabels))await field(label,value===null?'':String(value));
   await c.click('Save listing details');await saved();const sent=c.requests.at(-1);venue=await api('/venues/'+id,'GET',null,accounts.OWNER);
   for(const fieldName of Object.keys(booleanLabels)){assert.equal(sent[fieldName],value);assert.equal(venue[fieldName],value);const col=fieldName.replace(/[A-Z]/g,c=>'_'+c.toLowerCase());assert.equal(sql(`select coalesce(${col}::text,'NULL') from venues where id='${id}';`),value===null?'NULL':String(value));}
   await c.go(ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'reload');
   for(const label of Object.values(booleanLabels))assert.equal(await c.evaluate('Array.from(document.querySelectorAll("label")).find(l=>l.childNodes[0]?.textContent.trim()==='+JSON.stringify(label)+').querySelector("select").value'),value===null?'':String(value));
   await c.screen('owner-tristate-'+String(value));record('All eight tri-state controls → JSON → PostgreSQL → DTO → reload',{value});
 }
 await field('Maximum crew size','20');await field('Rules and considerations','QA TEST CONTENT — fictional venue rules');await field('Ceiling height (m)','3.5');
 await c.evaluate("(()=>{const f=Array.from(document.querySelectorAll('fieldset')).find(f=>f.querySelector('legend')?.textContent==='Permitted production activities');const e=f.querySelector('select');Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(e,'specified');e.dispatchEvent(new Event('change',{bubbles:true}));})()");
 await c.wait("Array.from(document.querySelectorAll('label')).some(l=>l.textContent.trim()==='Film'&&l.querySelector('input'))",'activities');await c.evaluate("Array.from(document.querySelectorAll('label')).find(l=>l.textContent.trim()==='Film').querySelector('input').click()");
 await c.click('Save listing details');await saved();
 const imageFile='/tmp/eema-venue-qa.png';fs.writeFileSync(imageFile,Buffer.from('iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAGUlEQVR4nGPUcNvHQApgIkn1qIZRDUNKAwDIDwFMvejdtwAAAABJRU5ErkJggg==','base64'));
 async function upload(){const document=await c.send('DOM.getDocument');const node=await c.send('DOM.querySelector',{nodeId:document.root.nodeId,selector:'input[type=file]'});await c.send('DOM.setFileInputFiles',{nodeId:node.nodeId,files:[imageFile]});await c.wait("document.body?.textContent.includes('Photo uploaded and attached')",'photo upload');}
 await upload();venue=await api('/venues/'+id,'GET',null,accounts.OWNER);assert.equal(venue.photoPaths.length,1);record('Production activity native persistence and actual photo upload');
 const verification=await api('/owner/verification/submit','POST',{note:'QA fictional owner review'},accounts.OWNER);await api('/admin/owner-verifications/'+verification.id+'/approve','POST',{verificationMethod:'MANUAL_REVIEW'},accounts.ADMIN);
 const ids=[];
 const types=fs.readFileSync(path.join(root,'src/main/java/com/eema/backend/entity/enums/PolicyType.java'),'utf8').match(/\b[A-Z][A-Z_]+\b/g).filter(x=>x!=='UUID');
 for(const policyType of [...new Set(types)]){
   const p=await api('/admin/policies','POST',{policyType,version:run+'-'+policyType,title:'QA TEST CONTENT — NOT LEGAL TERMS',content:'QA TEST CONTENT — NOT LEGAL TERMS',effectiveAt:'2099-01-01T00:00:00Z'},accounts.ADMIN,201);
   await api('/admin/policies/'+p.id+'/publish','POST',null,accounts.ADMIN);ids.push(p.id);
 }
 await api('/policies/accept','POST',{policyVersionIds:ids},accounts.OWNER);
 await login('OWNER',ownerRoute);await c.click('Submit for review');await c.wait("document.body?.textContent.includes('Listing under review')",'pending');await c.screen('owner-pending');record('Ready verified owner accepts exact current QA policies and submits');
 await login('ADMIN','/admin/venues');await c.wait('!!document.querySelector('+JSON.stringify('a[href="'+adminRoute+'"]')+')','admin queue');await c.go(adminRoute);await c.click('Request changes');await textarea('#reason','QA clarify loading arrangements');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Draft')&&!document.querySelector('#reason')",'changes requested');await c.screen('admin-changes');
 await login('OWNER',ownerRoute);await c.wait("document.body?.textContent.includes('QA clarify loading arrangements')",'owner reason');await field('Rules and considerations','QA clarified loading arrangements');await c.click('Save listing details');await saved();await c.click('Submit for review');await c.wait("document.body?.textContent.includes('Listing under review')",'resubmitted');
 await login('ADMIN',adminRoute);await c.click('Approve listing');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Live')&&!document.querySelector('#note')",'active');
 await login('OWNER',ownerRoute);await c.wait("document.body?.textContent.includes('Live on Eema')",'owner active');await c.click('Save listing details');await saved();assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'ACTIVE');record('Request changes → owner correction/resubmit → approve; identical numeric save stays ACTIVE');
 await field('Hourly rate','125');await c.click('Save listing details');await saved();assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'ACTIVE');
 await field('IANA timezone','America/New_York');await c.click('Save timezone');await c.wait("document.body?.textContent.includes('America/New_York')",'timezone save');
 await c.go(ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'reload');
 await c.evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='Add range').click()");await c.click('Save weekly hours');await c.wait("document.body?.textContent.includes('Saved successfully.')",'weekly save');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'ACTIVE');record('Noncritical price/timezone/weekly availability edits preserve ACTIVE');
 await field('Date','2099-01-15');await c.click('Add override');await c.wait("document.body?.textContent.includes('15 Jan 2099')||Array.from(document.querySelectorAll('button')).some(b=>b.textContent==='Delete')",'override saved');assert.equal((await api('/venues/'+id+'/availability/settings','GET',null,accounts.OWNER)).overrides.length,1);await c.click('Delete');await c.wait("document.body?.textContent.includes('No date overrides.')",'override removed');assert.equal((await api('/venues/'+id+'/availability/settings','GET',null,accounts.OWNER)).overrides.length,0);record('Owner creates and deletes availability override without changing ACTIVE');
 await field('Natural light','false');await c.click('Save listing details');await saved();await c.wait("document.body?.textContent.includes('Eema will review the listing again')",'critical notice');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');await c.screen('owner-critical-rereview');
 assert(!(await api('/venues','GET',null,null)).some(v=>v.id===id));record('Critical owner edit refetches PENDING, explains re-review and removes catalogue listing');
 await login('ADMIN',adminRoute);await c.click('Approve listing');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Live')&&!document.querySelector('#note')",'reapproved');
 await c.click('Suspend listing');await textarea('#reason','QA temporary venue suspension');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Suspended')&&!document.querySelector('#reason')",'suspended');await c.screen('admin-suspended');
 await login('OWNER',ownerRoute);await c.wait("document.body?.textContent.includes('Listing suspended')",'owner suspended');await login('ADMIN',adminRoute);await c.click('Reactivate listing');await c.click('Confirm');await c.wait("document.body?.textContent.includes('Live')&&!document.querySelector('#note')",'reactivated');record('Admin suspension/reactivation, owner reason/status and authoritative reload');
 // Public detailed tri-state rendering uses the existing ADMIN override contract; no invented approval of unknown required facts.
 for(const old of await api('/venues','GET',null,null)){if(old.id!==id){assert(old.name.startsWith('QA-VEN-'));await api('/admin/venues/'+old.id+'/suspend','POST',{reason:'QA isolate current catalogue fixture'},accounts.ADMIN);}}
 const publicLabels={naturalLight:'Natural light',blackoutPossible:'Blackout possible',loadingAccess:'Loading access',parkingAvailable:'Parking',liftAvailable:'Lift',wheelchairAccessible:'Wheelchair accessible',bathroomsAvailable:'Bathrooms',soundRecordingSuitable:'Sound recording'};
 for(const value of [null,false,true]){
   venue=await api('/venues/'+id,'GET',null,accounts.ADMIN);for(const fieldName of Object.keys(booleanLabels))venue[fieldName]=value;
   await api('/venues/'+id,'PUT',venue,accounts.ADMIN);
   await login('SCOUT','/');await c.wait('Array.from(document.querySelectorAll("button")).some(e=>e.textContent.includes('+JSON.stringify(run)+'))','catalogue card');
   await c.evaluate('Array.from(document.querySelectorAll("button")).find(e=>e.textContent.includes('+JSON.stringify(run)+')).click()');await c.wait("document.body?.textContent.includes('Production rules')",'public profile');
   for(const [fieldName,label] of Object.entries(publicLabels)){
     const expected=value===null?'Not specified':fieldName==='soundRecordingSuitable'?(value?'Suitable':'Not suitable'):(value?'Yes':'No');
     assert.equal(await c.evaluate('Array.from(document.querySelectorAll(".label")).find(e=>e.textContent==='+JSON.stringify(label)+')?.nextElementSibling?.textContent'),expected);
   }
   await c.screen('scout-tristate-'+String(value));record('SCOUT detailed profile preserves all eight tri-state meanings',{value,fixture:'ADMIN override on existing ACTIVE venue'});
 }
 await login('OWNER',ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'photos editor');
 await upload();await c.wait("document.body?.textContent.includes('Eema will review the listing again')",'photo addition review');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');
 await api('/admin/venues/'+id+'/approve','POST',{},accounts.ADMIN);
 await c.go(ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'photo reorder');await c.click('Move down');await c.wait("document.body?.textContent.includes('Photo order saved.')",'reordered');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');
 await api('/admin/venues/'+id+'/approve','POST',{},accounts.ADMIN);
 await c.go(ownerRoute);await c.wait("document.body?.textContent.includes('Edit details')",'photo removal');await c.evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='Remove'&&b.closest('section')?.textContent.includes('Listing media')).click()");await c.wait("document.body?.textContent.includes('Photo removed from the listing.')",'removed');assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');await api('/admin/venues/'+id+'/approve','POST',{},accounts.ADMIN);record('Photo addition, reorder and removal each trigger owner re-review');
 // Competing admin decision while a stale approval form remains open.
 for(let repeat=0;repeat<(process.argv.includes('--probe')?1:3);repeat++){
   venue=await api('/venues/'+id,'GET',null,accounts.OWNER);await api('/venues/'+id,'PUT',{...venue,productionRules:'QA race '+repeat},accounts.OWNER);
   await login('ADMIN',adminRoute);await c.click('Approve listing');await c.wait("!!document.querySelector('#note')",'stale form');await api('/admin/venues/'+id+'/request-changes','POST',{reason:'QA competing review'},accounts.ADMIN);
   await c.click('Confirm');await c.wait("document.body?.textContent.includes('Draft')",'conflict refetch');const visible=await c.evaluate("document.body?.textContent.includes('operation or record state changed')");
   if(process.argv.includes('--probe')){await c.screen('before-admin-stale-review');fs.writeFileSync(path.join(out,'ui-before.json'),JSON.stringify({result:visible?'NOT_REPRODUCED':'REPRODUCED',conflictMessageVisible:visible,state:'DRAFT'},null,2));await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');return;}
   assert.equal(visible,true);assert.equal(await c.evaluate("!!document.querySelector('#note')"),false);record('Stale admin decision keeps actionable 409 message after DRAFT refetch',{repeat});
   await api('/venues/'+id+'/submit-for-review','POST',null,accounts.OWNER);await api('/admin/venues/'+id+'/approve','POST',{},accounts.ADMIN);
 }
 await c.screen('admin-stale-review');
 await login('SCOUT',ownerRoute);await c.wait("document.body?.textContent.includes('This area is not available for your account')",'scout denial');
 await login('OWNER',adminRoute);await c.wait("document.body?.textContent.includes('ADMIN access required')",'owner admin denial');record('SCOUT owner-editor and OWNER admin-review guards');
 await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');
 fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'PASS',run,checks},null,2)+'\n');
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',run,checks,error:e.message},null,2));console.error(e.message);process.exit(1)});
