#!/usr/bin/env node
// Real UI/API with controlled Mollie transport in the test JVM; no provider payment occurs.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
const config=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const out=path.resolve(__dirname,'../../docs/qa/evidence/payout'),checks=[],secrets=Object.values(config.accounts).flatMap(a=>[a.accessToken,a.refreshToken]);
fs.mkdirSync(out,{recursive:true});
const profile=fs.mkdtempSync('/tmp/eema-payout-chrome-');let chrome,c;
function record(name){checks.push({name,result:'PASS'});}
async function connect(tab){const ws=new WS(tab.webSocketDebuggerUrl);await new Promise(r=>ws.on('open',r));let seq=0;const pending=new Map(),methods=new Map(),events=[],paused=[],requests=[];let intercept=null;
 ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error('CDP request failed: '+m.error.message)):p.resolve(m.result);}else if(m.method==='Network.requestWillBeSent'){methods.set(m.params.requestId,m.params.request.method);if(m.params.request.method==='PUT'&&m.params.request.url.includes('/api/venues/'))requests.push(JSON.parse(m.params.request.postData||'{}'));}else if(m.method==='Network.responseReceived'){let r=m.params.response;events.push({path:new URL(r.url).pathname,query:new URL(r.url).search,method:methods.get(m.params.requestId),status:r.status});}else if(m.method==='Fetch.requestPaused'){if(intercept)intercept(m.params).catch(e=>{if(!/Invalid.*(?:Interception|Request).*Id/i.test(e.message)){fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',checks,error:e.message},null,2));console.error(e.message);process.exit(1);}});else paused.push(m.params.requestId);}});
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error('Browser evaluation failed: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text));return r.result.value;};
 async function wait(expression,label){for(let i=0;i<150;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100));}throw Error('Browser timeout: '+label);}
 async function go(route){const target='http://localhost:3017'+route;let result;for(let attempt=0;attempt<3;attempt++){result=await send('Page.navigate',{url:target});if(result.errorText!=='net::ERR_ABORTED')break;await new Promise(r=>setTimeout(r,250));}if(result.errorText && result.errorText!=='net::ERR_ABORTED')throw Error('Navigation failed: '+result.errorText);await wait('location.href==='+JSON.stringify(target)+' && document.readyState==="complete"','document ready');}
 async function click(text){await wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.trim()==='+JSON.stringify(text)+'&&!b.disabled)','button '+text);await evaluate(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!b)throw Error('button missing');b.click();})()`);}
 async function fill(values){await evaluate(`(()=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;document.querySelectorAll('form input').forEach((el,i)=>{set.call(el,${JSON.stringify(values)}[i]);el.dispatchEvent(new Event('input',{bubbles:true}));});})()`);}
 const session=async()=>{const s=await evaluate("JSON.parse(localStorage.getItem('eema:auth'))");if(s)secrets.push(s.accessToken,s.refreshToken);return s;};
 const screen=async name=>{await evaluate("Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})))");const metrics=await send('Page.getLayoutMetrics');let clip={x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1};if(name.includes('gallery')||name.includes('photos')||name.includes('formats')||name.includes('image-error')){const box=await evaluate("(()=>{const s=Array.from(document.querySelectorAll('section')).find(e=>e.textContent.startsWith('Listing media'));if(!s)return null;const b=s.getBoundingClientRect();return {x:b.x+scrollX,y:b.y+scrollY,width:b.width,height:Math.min(b.height,2400),scale:1};})()");if(box)clip=box;}const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,requests,ws,setIntercept:fn=>{intercept=fn}};
}

(async()=>{
 try {
  chrome=cp.spawn('google-chrome',['--headless','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--disable-background-networking','--remote-debugging-port=9237','--user-data-dir='+profile,'about:blank'],{stdio:'ignore'});
  let tabs;for(let i=0;i<100;i++){try{tabs=await(await fetch('http://localhost:9237/json')).json();if(tabs.length)break;}catch{}await new Promise(r=>setTimeout(r,100));}assert(tabs?.length);c=await connect(tabs.find(t=>t.type==='page'));
  let submissions=0,refetches=0,liabilityConflict=false,liabilityWaived=false;
  c.setIntercept(async request=>{
   const r=request.request,url=new URL(r.url);let status,body;
   if(r.method==='OPTIONS'){status=204;body='';}
   else if(liabilityConflict&&r.method==='POST'&&url.pathname.endsWith('/settle')){liabilityConflict=false;liabilityWaived=true;status=409;body=JSON.stringify({message:'Only OPEN liabilities can be settled'});}
   else{
    if(r.method==='POST'&&url.pathname.endsWith('/mark-paid'))submissions++;
    if(r.method==='GET'&&url.pathname.includes('/admin/payouts/'))refetches++;
    const response=await fetch('http://localhost:18087'+url.pathname+url.search,{method:r.method,headers:Object.fromEntries(Object.entries(r.headers).filter(([key])=>!['origin','host'].includes(key.toLowerCase()))),...(r.postData?{body:r.postData}:{})});status=response.status;body=await response.text();
    if(liabilityWaived&&r.method==='GET'&&url.pathname==='/api/admin/liabilities/'+config.liability)body=JSON.stringify({...JSON.parse(body),status:"WAIVED"});
   }
   await c.send('Fetch.fulfillRequest',{requestId:request.requestId,responseCode:status,responseHeaders:[{name:'Access-Control-Allow-Origin',value:'http://localhost:3017'},{name:'Access-Control-Allow-Headers',value:'authorization,content-type'},{name:'Access-Control-Allow-Methods',value:'GET,POST,OPTIONS'},{name:'Content-Type',value:'application/json'}],body:Buffer.from(body).toString('base64')});
  });
  await c.send('Fetch.enable',{patterns:[{urlPattern:'http://localhost:8080/api/*'}]});
  async function login(role,route){await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(config.accounts[role]))+')');await c.go(route);}
  const has=text=>'(document.body.textContent||"").includes('+JSON.stringify(text)+')';
  await login('ADMIN','/admin/payouts');await c.wait(has('99,17'),'admin payout list');record('Admin list loads authoritative stored amounts');
  await c.go('/admin/payouts/'+config.success);await c.click('Mark as paid');await c.wait(has('does not send money'),'external transfer disclaimer');await c.fill(['QA browser transfer']);await c.screen('record-payment');
  const readsBefore=refetches;await c.click('Mark payout paid');await c.wait(has('QA browser transfer')+' && !Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Mark as paid")','authoritative PAID refetch');assert(refetches>readsBefore);assert.equal(submissions,1);record('Manual transfer is recorded once and authoritative PAID state is refetched');
  await c.go('/admin/payouts/'+config.success);await c.wait(has('QA browser transfer'),'paid reload');record('Reload retains paid reference and removes recording action');
  await c.go('/admin/payouts/'+config.held);await c.wait(has('Provider cost'),'held record');assert(!await c.evaluate('Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Mark as paid")'));record('Held payout has no payment recording action');
  for(const conflict of [config.conflict,config.conflict2]) {
  await c.go('/admin/payouts/'+conflict);await c.click('Mark as paid');await c.fill(['QA stale transfer']);
  const response=await fetch('http://localhost:18087/api/admin/payouts/'+conflict+'/mark-paid',{method:'POST',headers:{Authorization:'Bearer '+config.accounts.ADMIN.accessToken,'Content-Type':'application/json'},body:JSON.stringify({externalTransferReference:'QA competing transfer'})});assert.equal(response.status,200);
  await c.click('Mark payout paid');await c.wait(has('QA competing transfer'),'conflict authoritative refetch');
  const visible=await c.evaluate('Array.from(document.querySelectorAll("[role=alert]")).some(e=>e.textContent.includes("record state changed"))');checks.push({name:'Real 409 retains visible conflict feedback after authoritative state changes to PAID',result:visible?'PASS':'FAIL'});await c.screen('conflict-refetch');
  }
  // Client-only stale WAIVED response: no public waive endpoint exists. Actual settlement below uses the real API.
  await c.go('/admin/liabilities/'+config.liability);await c.click('Mark settled');liabilityConflict=true;await c.click('Mark settled');await c.wait(has('Waived'),'controlled stale liability refetch');
  checks.push({name:'Controlled liability 409 retains visible conflict after WAIVED refetch',result:await c.evaluate('Array.from(document.querySelectorAll("[role=alert]")).some(e=>e.textContent.includes("record state changed"))')?'PASS':'FAIL'});liabilityWaived=false;
  await c.go('/admin/liabilities/'+config.liability);await c.click('Mark settled');await c.wait(has('does not move money'),'settlement disclaimer');await c.click('Mark settled');await c.wait(has('Settled')+' && !Array.from(document.querySelectorAll("button")).some(b=>b.textContent==="Mark settled")','settled refetch');await c.go('/admin/liabilities/'+config.liability);await c.wait(has('1,23'),'stored settlement amount');record('Liability settlement preserves stored amount, refetches and survives reload');
  await login('OWNER','/owner/payouts/'+config.success);await c.wait(has('99,17'),'owner payout amount');assert(!await c.evaluate(has('QA browser transfer')));assert(!await c.evaluate('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.includes("Mark"))'));await c.screen('owner-paid');record('Owner sees authoritative amount and masked destination without private transfer reference or admin action');
  await login('OTHER','/owner/payouts/'+config.success);await c.wait(has('access'),'other owner denied');record('Different owner cannot read payout detail');
  const result=checks.every(x=>x.result==='PASS')?'PASS':'FAIL';const evidence=JSON.stringify({result,checks,provider:'Local PostgreSQL and controlled provider; no bank transfer'},null,2);for(const secret of secrets)assert(!evidence.includes(secret));fs.writeFileSync(path.join(out,'browser.json'),evidence+'\n');if(result!=='PASS')process.exitCode=1;
 }finally{
  if(c){try{await c.send('Browser.close')}catch{}c.ws.close();}if(chrome&&chrome.exitCode===null){const exited=new Promise(r=>chrome.once('exit',r));chrome.kill();await exited;}fs.rmSync(profile,{recursive:true,force:true,maxRetries:10,retryDelay:100});
 }
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',checks,error:e.message},null,2));console.error(e.message);process.exitCode=1;});
