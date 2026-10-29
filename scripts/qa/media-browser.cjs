#!/usr/bin/env node
// Reuses AUTH/POL/TRUST Chrome CDP + ws. Dedicated fictional venue fixtures only.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
if(process.argv[2]!=='--local-qa')throw Error('Use --local-qa');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'docs/qa/evidence/media'),run='QA-MEDIA-'+Date.now(),password='QaFixture-2026!',checks=[],secrets=[];
fs.mkdirSync(out,{recursive:true});
const compose=['compose','-p','eema-media-qa','-f',path.join(root,'scripts/qa/media-compose.yml')];
function sql(query){return cp.execFileSync('docker',[...compose,'exec','-T','postgres','psql','-X','-U','eema','-d','eema_media_qa','-At','-v','ON_ERROR_STOP=1'],{input:query,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();}
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
 const screen=async name=>{const metrics=await send('Page.getLayoutMetrics');let clip={x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1};if(name.includes('gallery')||name.includes('photos')||name.includes('formats')||name.includes('image-error')){const box=await evaluate("(()=>{const s=Array.from(document.querySelectorAll('section')).find(e=>e.textContent.startsWith('Listing media'));if(!s)return null;const b=s.getBoundingClientRect();return {x:b.x+scrollX,y:b.y+scrollY,width:b.width,height:Math.min(b.height,2400),scale:1};})()");if(box)clip=box;}const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,requests,ws,setIntercept:fn=>{intercept=fn}};
}

(async()=>{
 assert.equal(sql('select current_database()'),'eema_media_qa');
 let healthy=false;for(let i=0;i<120;i++){try{if((await fetch('http://localhost:8080/api/actuator/health',{signal:AbortSignal.timeout(1000)})).ok){healthy=true;break;}}catch{}await new Promise(r=>setTimeout(r,500));}assert(healthy,'backend health');
 const accounts={};
 for(const role of ['OWNER','ADMIN','SCOUT','OTHER']){
  const a=await api('/auth/register','POST',{email:run+'-'+role+'@example.test',fullName:'QA Media '+role,password,role:role==='ADMIN'?'SCOUT':role==='OTHER'?'OWNER':role},null,201);
  sql(`update users set email_verified=true${role==='ADMIN'?",role='ADMIN'":''} where id='${a.user.id}';`);
  accounts[role]=await api('/auth/login','POST',{email:a.user.email,password},null);
 }
 // The home page features the first ACTIVE venue. Isolate this run using supported moderation, retaining prior QA history.
 for(const old of await api('/venues','GET',null,null)){
  assert(old.name.startsWith('QA-MEDIA-'),'only disposable MEDIA fixtures may be changed');
  await api('/admin/venues/'+old.id+'/suspend','POST',{reason:'QA isolate current browser fixture'},accounts.ADMIN);
 }
 const c=await connect((await(await fetch('http://localhost:9223/json')).json())[0]);
 const imageRoot=path.join(root,'src/test/resources/media');
 async function rawUpload(route,bytes,name,type,account,expected=200){const form=new FormData();form.append('file',new Blob([bytes],{type}),name);const response=await fetch('http://localhost:8080/api'+route,{method:'POST',headers:account?{Authorization:'Bearer '+account.accessToken}:{},body:form});assert.equal(response.status,expected,'upload '+name);return response.json();}
 async function login(role,route){await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(accounts[role]))+')');await c.go(route);}
 async function upload(name,count){
  const doc=await c.send('DOM.getDocument');const input=await c.send('DOM.querySelector',{nodeId:doc.root.nodeId,selector:'input[type=file]'});
  await c.evaluate("document.querySelector('input[type=file]').scrollIntoView({block:'center'})");
  await c.send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[path.join(imageRoot,name)]});
  await c.wait(photoImages+'.length==='+count,'photo count');
  await c.wait("document.body?.textContent.includes('Photo uploaded and attached')",'upload acknowledgement');
 }
 async function chosenFile(file){const doc=await c.send('DOM.getDocument');const input=await c.send('DOM.querySelector',{nodeId:doc.root.nodeId,selector:'input[type=file]'});await c.send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[file]});}
 const photoImages="Array.from(document.querySelectorAll('img')).filter(e=>e.alt.includes(' photo '))";
 async function previews(count){await c.wait(photoImages+'.length==='+count,'gallery rendered');await c.evaluate('(async()=>{for(const e of '+photoImages+'){e.scrollIntoView({block:"center"});await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));}})()');await c.wait(photoImages+'.length==='+count+' && '+photoImages+'.every(e=>e.complete&&e.naturalWidth===16)','decoded image previews');}
 await c.go('/');await c.evaluate("localStorage.removeItem('eema:auth')");await c.go('/');await c.click('Sign in / register');await c.wait("!!document.querySelector('form input')",'login');await c.fill([accounts.OWNER.user.email,password]);await c.evaluate("document.querySelector('form button[type=submit]').click()");await c.wait("document.querySelector('header')?.textContent.includes('QA Media OWNER')",'owner signed in');accounts.OWNER=await c.session();
 const v=await api('/venues','POST',{name:run,neighbourhood:'QA District',sizeM2:100,tags:[],hourlyRateEur:100,minimumBookingMinutes:60,minimumLeadTimeMinutes:60},accounts.OWNER,201);
 const id=v.id,route='/venues/'+id+'/photos',ownerRoute='/owner/venues/'+id;
 await c.go(ownerRoute);await c.wait("!!document.querySelector('input[type=file]')",'upload editor');
 assert.equal(await c.evaluate("document.querySelector('input[type=file]').accept"),'image/jpeg,image/png,image/webp');
 // Pause an actual upload request only to observe the pending UI, then release it to Spring.
 await c.send('Fetch.enable',{patterns:[{urlPattern:'*/api/venues/*/photos',requestStage:'Request'}]});
 const pendingUpload=upload('qa.jpg',1);
 for(let i=0;i<100&&!c.paused.length;i++)await new Promise(r=>setTimeout(r,100));assert.equal(c.paused.length,1);
 assert.equal(await c.evaluate("document.body.textContent.includes('Uploading and attaching photo…') && document.querySelector('input[type=file]').disabled"),true);
 await c.send('Fetch.continueRequest',{requestId:c.paused.shift()});await c.send('Fetch.disable');await pendingUpload;await previews(1);
 await upload('qa.png',2);await previews(2);await upload('qa.webp',3);await previews(3);
 let venue=await api('/venues/'+id,'GET',null,accounts.OWNER),refs=[...venue.photoPaths];assert.equal(refs.length,3);
 await c.screen('owner-three-formats');record('Real owner login and JPEG/PNG/WebP browser uploads; loading state, valid previews, ordered persisted references');
 await c.go(ownerRoute);await previews(3);assert.equal(await c.evaluate(photoImages+'[0].src'), 'http://localhost:8080'+refs[0]);
 await c.evaluate("Array.from(document.querySelectorAll('button')).filter(b=>b.textContent==='Move up')[2].click()");await c.wait("document.body.textContent.includes('Photo order saved.')",'first reorder');
 await c.evaluate("Array.from(document.querySelectorAll('button')).filter(b=>b.textContent==='Move up')[1].click()");await c.wait("document.body.textContent.includes('Primary photo updated.')",'primary reordered');
 venue=await api('/venues/'+id,'GET',null,accounts.OWNER);assert.deepEqual(venue.photoPaths,[refs[2],refs[0],refs[1]]);
 await c.evaluate("Array.from(document.querySelectorAll('button')).filter(b=>b.textContent==='Remove')[1].click()");await c.wait("document.body.textContent.includes('Photo removed from the listing.')",'removed');
 assert.deepEqual((await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths,[refs[2],refs[1]]);
 assert.equal((await fetch('http://localhost:8080'+refs[0])).status,200);record('Reload preserves order; move changes primary; removal detaches reference but keeps physical object');
 const fake='/tmp/eema-media-fake.jpg',large='/tmp/eema-media-large.jpg';fs.writeFileSync(fake,'this is not a jpeg');const padded=Buffer.alloc(5242881);fs.readFileSync(path.join(imageRoot,'qa.jpg')).copy(padded);fs.writeFileSync(large,padded);
 await chosenFile(fake);await c.wait("document.querySelector('[role=alert]')?.textContent.includes('Invalid or corrupt image')",'corrupt error');
 assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths.length,2);await c.screen('invalid-image-error');
 await chosenFile(large);await c.wait("!!document.querySelector('[role=alert]') && !document.body.textContent.includes('Uploading and attaching')",'oversize error');
 const sizeMessage=await c.evaluate("document.querySelector('[role=alert]').textContent");
 if(process.argv.includes('--probe')){fs.writeFileSync(path.join(out,'size-http-probe.json'),JSON.stringify({http:c.events.filter(e=>e.path.endsWith('/photos')).at(-1)?.status,message:sizeMessage},null,2));await c.send('Browser.close');return;}
 assert.match(sizeMessage,/too large|maximum size|size limit/i);record('Corrupt image rejected with actionable error; actual servlet oversize returns 413 with size guidance');
 for(const [role,status] of [['OTHER',403],['SCOUT',403],[null,401]])await rawUpload(route,fs.readFileSync(path.join(imageRoot,'qa.jpg')),'qa.jpg','image/jpeg',accounts[role],status);
 for(const [role,status] of [['ADMIN',403],['SCOUT',403],[null,401]])await rawUpload('/upload/venue-photo',fs.readFileSync(path.join(imageRoot,'qa.jpg')),'qa.jpg','image/jpeg',accounts[role],status);
 await rawUpload(route,fs.readFileSync(path.join(imageRoot,'qa.jpeg')),'../../qa.jpeg','image/jpeg',accounts.ADMIN);
 const standalone=await rawUpload('/upload/venue-photo',fs.readFileSync(path.join(imageRoot,'qa.jpg')),'qa.jpg','image/jpeg',accounts.OWNER);assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths.length,3);
 const exact=padded.subarray(0,5242880);await rawUpload('/upload/venue-photo',exact,'qa.jpg','image/jpeg',accounts.OWNER);
 await rawUpload('/upload/venue-photo',padded,'qa.jpg','image/jpeg',accounts.OWNER,413);
 for(const bytes of [Buffer.alloc(0),Buffer.from([1]),Buffer.from('fake')])await rawUpload('/upload/venue-photo',bytes,'qa.jpg','image/jpeg',accounts.OWNER,400);
 const missing=await fetch('http://localhost:8080/api/upload/venue-photo',{method:'POST',headers:{Authorization:'Bearer '+accounts.OWNER.accessToken},body:new FormData()});assert.equal(missing.status,400);
 const malformed=await fetch('http://localhost:8080/api/upload/venue-photo',{method:'POST',headers:{Authorization:'Bearer '+accounts.OWNER.accessToken,'Content-Type':'multipart/form-data'},body:'broken'});assert.equal(malformed.status,400);
 for(const suffix of ['00000000-0000-0000-0000-000000000001.jpg','private.pdf','%2e%2e%2fprivate.pdf'])assert([400,404].includes((await fetch('http://localhost:8080/api/upload/files/'+suffix)).status));
 const read=await fetch('http://localhost:8080'+standalone.url);assert.equal(read.status,200);assert.equal(read.headers.get('content-type'),'image/jpeg');assert.equal(read.headers.get('x-content-type-options'),'nosniff');
 record('Real Tomcat HTTP boundaries and actor matrix; exact 5 MiB accepted, +1 rejected; standalone does not attach; public reads have correct MIME/nosniff',{cacheControl:read.headers.get('cache-control')});
 // Establish a real approved listing solely to verify public primary-photo rendering and photo re-review.
 const review=await api('/owner/verification/submit','POST',{note:'QA fictional owner review'},accounts.OWNER);
 await api('/admin/owner-verifications/'+review.id+'/approve','POST',{verificationMethod:'MANUAL_REVIEW'},accounts.ADMIN);
 const policyIds=[];
 for(const policyType of ['TERMS_OF_SERVICE','PRIVACY_POLICY','ACCEPTABLE_USE_POLICY','PRODUCTION_POLICY','INSURANCE_REQUIREMENTS']){
  const policy=await api('/admin/policies','POST',{policyType,version:run+'-'+policyType,title:'QA TEST CONTENT — NOT LEGAL TERMS',content:'QA TEST CONTENT — NOT LEGAL TERMS',effectiveAt:'2099-01-01T00:00:00Z'},accounts.ADMIN,201);
  await api('/admin/policies/'+policy.id+'/publish','POST',null,accounts.ADMIN);policyIds.push(policy.id);
 }
 await api('/policies/accept','POST',{policyVersionIds:policyIds},accounts.OWNER);
 venue=await api('/venues/'+id,'GET',null,accounts.OWNER);
 await api('/venues/'+id,'PUT',{...venue,maxCrewSize:20,permittedActivities:['FILM'],productionRules:'QA fictional production rules',naturalLight:false,loadingAccess:false,bathroomsAvailable:false,soundRecordingSuitable:false},accounts.OWNER);
 await api('/venues/'+id+'/submit-for-review','POST',{},accounts.OWNER);
 await api('/admin/venues/'+id+'/approve','POST',{},accounts.ADMIN);
 const primary=(await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths[0];
 const summary=(await api('/venues','GET',null,null)).find(v=>v.id===id);assert.equal(summary.firstPhotoUrl,primary);
 await login('SCOUT','/');await c.wait('Array.from(document.querySelectorAll("button")).some(e=>e.textContent.includes('+JSON.stringify(run)+'))','public card');
 let publicImage='Array.from(document.querySelectorAll("img")).find(e=>e.alt==='+JSON.stringify(run)+')';
 await c.wait('!!('+publicImage+')','public primary');await c.evaluate(publicImage+'.scrollIntoView({block:"center"})');
 await c.wait(publicImage+'.complete && '+publicImage+'.naturalWidth===16','public primary decoded');assert.equal(await c.evaluate(publicImage+'.src'),'http://localhost:8080'+primary);
 await c.evaluate('Array.from(document.querySelectorAll("button")).find(e=>e.textContent.includes('+JSON.stringify(run)+')).click()');
 await c.wait("document.body.textContent.includes('Production rules')",'public detail');
 await c.wait("!!document.querySelector('img')",'detail gallery');assert.equal(await c.evaluate("document.querySelector('img').src"),'http://localhost:8080'+primary);await c.screen('scout-primary-detail');
 await login('OWNER',ownerRoute);await upload('qa.jpg',4);await previews(4);await c.wait("document.body.textContent.includes('Eema will review the listing again')",'active photo rereview');
 assert.equal((await api('/venues/'+id,'GET',null,accounts.OWNER)).status,'PENDING');assert(!(await api('/venues','GET',null,null)).some(v=>v.id===id));
 record('Public catalogue/detail use first photo; approved listing photo addition refetches PENDING and removes public listing');
 while((await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths.length<12)await rawUpload(route,fs.readFileSync(path.join(imageRoot,'qa.png')),'qa.png','image/png',accounts.OWNER);
 await c.go(ownerRoute);await previews(12);assert.equal(await c.evaluate("document.querySelector('input[type=file]').disabled"),true);
 await rawUpload(route,fs.readFileSync(path.join(imageRoot,'qa.png')),'qa.png','image/png',accounts.OWNER,400);await c.screen('owner-twelve-photos');record('12-photo UI disables upload and backend independently rejects thirteenth');
 await c.send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await c.screen('owner-mobile-gallery');await c.send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});
 venue=await api('/venues/'+id,'GET',null,accounts.OWNER);const before=[...venue.photoPaths];
 cp.execFileSync('docker',[...compose,'restart','backend'],{stdio:['ignore','pipe','pipe']});
 let ready=false;for(let i=0;i<120;i++){try{if((await fetch('http://localhost:8080/api/actuator/health',{signal:AbortSignal.timeout(1000)})).ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,500));}assert(ready,'backend restart health');
 assert.deepEqual((await api('/venues/'+id,'GET',null,accounts.OWNER)).photoPaths,before);for(const ref of before)assert.equal((await fetch('http://localhost:8080'+ref)).status,200);
 await c.go(ownerRoute);await previews(12);record('Production Dockerfile backend container restart retains all references and bytes in dedicated named media volume');
 await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');
 fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'PASS',run,checks},null,2)+'\n');
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',run,checks,error:e.message},null,2));console.error(e.message);process.exit(1)});
