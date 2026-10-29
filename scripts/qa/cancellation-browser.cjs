#!/usr/bin/env node
// Real UI/API with controlled Mollie transport in the test JVM; no provider payment occurs.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
const config=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const out=path.resolve(__dirname,'../../docs/qa/evidence/cancellation'),checks=[],secrets=Object.values(config.accounts).flatMap(a=>[a.accessToken,a.refreshToken]);
fs.mkdirSync(out,{recursive:true});
const profile=fs.mkdtempSync('/tmp/eema-cancellation-chrome-');let chrome,c;
function record(name){checks.push({name,result:'PASS'});}
async function connect(tab){const ws=new WS(tab.webSocketDebuggerUrl);await new Promise(r=>ws.on('open',r));let seq=0;const pending=new Map(),methods=new Map(),events=[],paused=[],requests=[];let intercept=null;
 ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error('CDP request failed')):p.resolve(m.result);}else if(m.method==='Network.requestWillBeSent'){methods.set(m.params.requestId,m.params.request.method);if(m.params.request.method==='PUT'&&m.params.request.url.includes('/api/venues/'))requests.push(JSON.parse(m.params.request.postData||'{}'));}else if(m.method==='Network.responseReceived'){let r=m.params.response;events.push({path:new URL(r.url).pathname,query:new URL(r.url).search,method:methods.get(m.params.requestId),status:r.status});}else if(m.method==='Fetch.requestPaused'){if(intercept)intercept(m.params);else paused.push(m.params.requestId);}});
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error('Browser evaluation failed: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text));return r.result.value;};
 async function wait(expression,label){for(let i=0;i<150;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100));}throw Error('Browser timeout: '+label);}
 async function go(route){const target='http://localhost:3016'+route;let result;for(let attempt=0;attempt<3;attempt++){result=await send('Page.navigate',{url:target});if(result.errorText!=='net::ERR_ABORTED')break;await new Promise(r=>setTimeout(r,250));}if(result.errorText && result.errorText!=='net::ERR_ABORTED')throw Error('Navigation failed: '+result.errorText);await wait('location.href==='+JSON.stringify(target)+' && document.readyState==="complete"','document ready');}
 async function click(text){await wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.trim()==='+JSON.stringify(text)+'&&!b.disabled)','button '+text);await evaluate(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!b)throw Error('button missing');b.click();})()`);}
 async function fill(values){await evaluate(`(()=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;document.querySelectorAll('form input').forEach((el,i)=>{set.call(el,${JSON.stringify(values)}[i]);el.dispatchEvent(new Event('input',{bubbles:true}));});})()`);}
 const session=async()=>{const s=await evaluate("JSON.parse(localStorage.getItem('eema:auth'))");if(s)secrets.push(s.accessToken,s.refreshToken);return s;};
 const screen=async name=>{await evaluate("Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})))");const metrics=await send('Page.getLayoutMetrics');let clip={x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1};if(name.includes('gallery')||name.includes('photos')||name.includes('formats')||name.includes('image-error')){const box=await evaluate("(()=>{const s=Array.from(document.querySelectorAll('section')).find(e=>e.textContent.startsWith('Listing media'));if(!s)return null;const b=s.getBoundingClientRect();return {x:b.x+scrollX,y:b.y+scrollY,width:b.width,height:Math.min(b.height,2400),scale:1};})()");if(box)clip=box;}const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,requests,ws,setIntercept:fn=>{intercept=fn}};
}

(async()=>{
 try {
  chrome=cp.spawn('google-chrome',['--headless','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--disable-background-networking','--remote-debugging-port=9236','--user-data-dir='+profile,'about:blank'],{stdio:'ignore'});
  let tabs;for(let i=0;i<100;i++){try{tabs=await(await fetch('http://localhost:9236/json')).json();if(tabs.length)break;}catch{}await new Promise(r=>setTimeout(r,100));}assert(tabs?.length);
  c=await connect(tabs.find(t=>t.type==='page'));
  let failPreview=false,failSubmit=false,failConflict=false,submits=0,refreshes=0;
  c.setIntercept(async request=>{
   const r=request.request,url=new URL(r.url);
   let status,body,headers=[{name:'Access-Control-Allow-Origin',value:'http://localhost:3016'},{name:'Access-Control-Allow-Headers',value:'authorization,content-type'},{name:'Access-Control-Allow-Methods',value:'GET,POST,OPTIONS'}];
   if(r.method==='OPTIONS'){status=204;body='';}
   else if(failPreview&&url.pathname.endsWith('/cancellation-preview')){failPreview=false;status=503;body=JSON.stringify({message:'QA preview unavailable'});}
   else if(failConflict&&url.pathname.endsWith('/cancel')){failConflict=false;submits++;status=409;body=JSON.stringify({code:'CANCELLATION_TERMS_CHANGED',message:'QA stale preview'});}
   else if(failSubmit&&url.pathname.endsWith('/cancel')){submits++;status=401;body=JSON.stringify({message:'QA expired session'});}
   else {
    if(url.pathname.endsWith('/cancel')){submits++;const sent=JSON.parse(r.postData);assert(sent.previewToken,'No paid cancellation without proof');}
    if(url.pathname.endsWith('/auth/refresh'))refreshes++;
    const response=await fetch('http://localhost:18086'+url.pathname+url.search,{method:r.method,headers:Object.fromEntries(Object.entries(r.headers).filter(([key])=>!['origin','host'].includes(key.toLowerCase()))),...(r.postData?{body:r.postData}:{})});status=response.status;body=await response.text();
   }
   headers.push({name:'Content-Type',value:'application/json'});
   await c.send('Fetch.fulfillRequest',{requestId:request.requestId,responseCode:status,responseHeaders:headers,body:Buffer.from(body).toString('base64')});
  });
  await c.send('Fetch.enable',{patterns:[{urlPattern:'http://localhost:8080/api/*'}]});
  async function login(route){await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(config.accounts.SCOUT))+')');await c.go(route);}
  await login('/bookings/'+config.success);
  failPreview=true;await c.click('Cancel booking');await c.wait('(document.body.textContent||"").includes("could not be confirmed")','preview failure');
  assert(!await c.evaluate('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.includes("Confirm cancel")&&!b.disabled)'));record('Preview failure cannot enable paid cancellation');
  await c.click('Cancel booking');await c.wait('(document.body.textContent||"").includes("125,50") && Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Confirm cancel booking"&&!b.disabled)','authoritative preview');
  assert(!await c.evaluate('!!document.querySelector("input[type=checkbox]:checked")'));await c.screen('confirmation');record('Preview retry loads server amounts, explicit CTA and keep-booking action');
  failConflict=true;await c.click('Confirm cancel booking');await c.wait('(document.body.textContent||"").includes("terms changed while") && Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Confirm cancel booking"&&!b.disabled)','409 refetch and renewed consent');assert.equal(submits,1);record('409 refetches preview, shows conflict and requires another explicit confirmation');
  await c.evaluate('(()=>{const b=Array.from(document.querySelectorAll("button")).find(b=>b.textContent==="Confirm cancel booking");b.click();b.click();})()');
  await c.wait('(document.body.textContent||"").includes("Cancellation details") && (document.body.textContent||"").includes("Refund in progress") && !Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Cancel booking")','authoritative cancellation refetch');assert.equal(submits,2);record('Duplicate clicks submit once and authoritative booking is refetched');
  await c.go('/bookings/'+config.success);await c.wait('(document.body.textContent||"").includes("Cancellation details") && (document.body.textContent||"").includes("Refund in progress")','reload authoritative cancellation');record('Reload preserves cancelled state and pending refund distinction');
  await login('/bookings/'+config.zero);await c.click('Cancel booking');await c.wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Cancel without refund")','zero refund warning');await c.screen('zero-refund');await c.click('Keep booking');record('Zero refund has explicit destructive consequence and back action');
  await login('/bookings/'+config.expiry);await c.click('Cancel booking');const beforeRefresh=refreshes;failSubmit=true;await c.click('Confirm cancel booking');await c.wait('(document.body.textContent||"").includes("session has expired")','session expiry');assert.equal(submits,3);assert.equal(refreshes,beforeRefresh);failSubmit=false;record('401 mutation is not refreshed and automatically replayed');
  await login('/bookings/'+config.session);await c.click('Cancel booking');await c.wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Confirm cancel booking"&&!b.disabled)','session preview loaded');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(config.accounts.SECOND))+');window.dispatchEvent(new StorageEvent("storage",{key:"eema:auth"}))');
  await c.wait('!Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Confirm cancel booking"&&!b.disabled)','account switch clears confirmation');assert.equal(submits,3);record('Account switch invalidates pending confirmation');
  const evidence=JSON.stringify({result:'PASS',checks,provider:'Mocked Mollie in disposable PostgreSQL fixture; not live provider verification'},null,2);for(const secret of secrets)assert(!evidence.includes(secret));fs.writeFileSync(path.join(out,'browser.json'),evidence+'\n');
 } finally {
  if(c){try{await c.send('Browser.close')}catch{}c.ws.close();}
  if(chrome&&chrome.exitCode===null){const exited=new Promise(r=>chrome.once('exit',r));chrome.kill();await exited;}
  fs.rmSync(profile,{recursive:true,force:true,maxRetries:10,retryDelay:100});
 }
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',checks,error:e.message},null,2));console.error(e.message);process.exitCode=1;});
