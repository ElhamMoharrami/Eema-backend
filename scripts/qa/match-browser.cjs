#!/usr/bin/env node
// Existing Chrome CDP harness; real UI/API with controlled Gemini transport in the test JVM.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
const config=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const out=path.resolve(__dirname,'../../docs/qa/evidence/match'),checks=[],secrets=[config.account.accessToken,config.account.refreshToken];
fs.mkdirSync(out,{recursive:true});
const profile=fs.mkdtempSync('/tmp/eema-match-chrome-');let chrome,c;
function record(name){checks.push({name,result:'PASS'});}
async function connect(tab){const ws=new WS(tab.webSocketDebuggerUrl);await new Promise(r=>ws.on('open',r));let seq=0;const pending=new Map(),methods=new Map(),events=[],paused=[],requests=[];let intercept=null;
 ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error('CDP request failed')):p.resolve(m.result);}else if(m.method==='Network.requestWillBeSent'){methods.set(m.params.requestId,m.params.request.method);if(m.params.request.method==='PUT'&&m.params.request.url.includes('/api/venues/'))requests.push(JSON.parse(m.params.request.postData||'{}'));}else if(m.method==='Network.responseReceived'){let r=m.params.response;events.push({path:new URL(r.url).pathname,query:new URL(r.url).search,method:methods.get(m.params.requestId),status:r.status});}else if(m.method==='Fetch.requestPaused'){if(intercept)intercept(m.params);else paused.push(m.params.requestId);}});
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error('Browser evaluation failed: '+(r.exceptionDetails.exception?.description||r.exceptionDetails.text));return r.result.value;};
 async function wait(expression,label){for(let i=0;i<150;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100));}throw Error('Browser timeout: '+label);}
 async function go(route){const target='http://localhost:3000'+route;let result;for(let attempt=0;attempt<3;attempt++){result=await send('Page.navigate',{url:target});if(result.errorText!=='net::ERR_ABORTED')break;await new Promise(r=>setTimeout(r,250));}if(result.errorText && result.errorText!=='net::ERR_ABORTED')throw Error('Navigation failed: '+result.errorText);await wait('location.href==='+JSON.stringify(target)+' && document.readyState==="complete"','document ready');}
 async function click(text){await wait('Array.from(document.querySelectorAll("button")).some(b=>b.textContent.trim()==='+JSON.stringify(text)+'&&!b.disabled)','button '+text);await evaluate(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!b)throw Error('button missing');b.click();})()`);}
 async function fill(values){await evaluate(`(()=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;document.querySelectorAll('form input').forEach((el,i)=>{set.call(el,${JSON.stringify(values)}[i]);el.dispatchEvent(new Event('input',{bubbles:true}));});})()`);}
 const session=async()=>{const s=await evaluate("JSON.parse(localStorage.getItem('eema:auth'))");if(s)secrets.push(s.accessToken,s.refreshToken);return s;};
 const screen=async name=>{const metrics=await send('Page.getLayoutMetrics');let clip={x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1};if(name.includes('gallery')||name.includes('photos')||name.includes('formats')||name.includes('image-error')){const box=await evaluate("(()=>{const s=Array.from(document.querySelectorAll('section')).find(e=>e.textContent.startsWith('Listing media'));if(!s)return null;const b=s.getBoundingClientRect();return {x:b.x+scrollX,y:b.y+scrollY,width:b.width,height:Math.min(b.height,2400),scale:1};})()");if(box)clip=box;}const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,requests,ws,setIntercept:fn=>{intercept=fn}};
}

(async()=>{
 try {
  chrome=cp.spawn('google-chrome',['--headless','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--disable-background-networking','--remote-debugging-port=9223','--user-data-dir='+profile,'about:blank'],{stdio:'ignore'});
  let tabs;for(let i=0;i<100;i++){try{tabs=await(await fetch('http://localhost:9223/json')).json();if(tabs.length)break;}catch{}await new Promise(r=>setTimeout(r,100));}assert(tabs?.length);
  c=await connect(tabs.find(t=>t.type==='page'));await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(config.account))+')');await c.go('/');
  async function search(brief){await c.wait('!!document.querySelector("textarea")','search input');await c.evaluate('(()=>{const e=document.querySelector("textarea");Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,"value").set.call(e,'+JSON.stringify(brief)+');e.dispatchEvent(new Event("input",{bubbles:true}));})()');await c.click('Find venues');}
  // Pause the actual outbound request to make the loading state observable without a timing race.
  await c.send('Fetch.enable',{patterns:[{urlPattern:'*/api/match',requestStage:'Request'}]});
  await search('QA SIMPLE photo production for twelve people');await c.wait('document.body.textContent.includes("Eema is reading your brief")','loading');await c.wait('true','request pending');
  for(let i=0;i<100&&!c.paused.length;i++)await new Promise(r=>setTimeout(r,50));assert(c.paused.length);await c.send('Fetch.continueRequest',{requestId:c.paused.pop()});await c.send('Fetch.disable');
  await c.wait('document.body.textContent.includes("1 matches")','results');assert.equal(await c.evaluate('window.qaInjected===true'),false);assert(await c.evaluate('document.body.textContent.includes("<script>window.qaInjected=true</script>")'));await c.screen('results-text-only');record('Simple brief: loading, real backend results, authoritative pricing and model explanation rendered as text');
  await c.evaluate('Array.from(document.querySelectorAll("button")).find(b=>b.textContent.includes("View · Get quote")).click()');await c.wait('document.body.textContent.includes("Availability") || document.querySelector("input[type=time]")','full venue detail');assert(c.events.some(e=>e.path==='/api/venues/'+config.venueId&&e.status===200));await c.screen('venue-detail');record('Match card opens full authoritative venue detail');
  for(const [brief,label] of [['QA EMPTY photo production for many people','Zero hard-eligible candidates'],['QA UNAVAILABLE exact time outside operating hours','Exact unavailable interval']]){
   await c.go('/');await search(brief);await c.wait('document.body.textContent.includes("No matching venues")','empty results');assert(!await c.evaluate('document.body.textContent.includes("1 matches")'));record(label+': clear empty state without relaxed constraints');
  }await c.screen('empty-results');
  await c.go('/');await search('QA PROVIDER failure fixture');await c.wait('document.body.textContent.includes("temporarily unavailable")','safe provider failure');assert(!await c.evaluate('document.body.textContent.includes("SENTINEL")'));record('Provider failure: safe actionable error, no raw provider payload');
  await c.send('Fetch.enable',{patterns:[{urlPattern:'*/api/match',requestStage:'Request'}]});c.setIntercept(p=>void c.send('Fetch.failRequest',{requestId:p.requestId,errorReason:'Failed'}));await search('QA NETWORK failure fixture');await c.wait('document.body.textContent.includes("connectivity")','network failure');await c.send('Fetch.disable');c.setIntercept(null);record('Network failure: actionable retry state');
  await c.screen('safe-errors');await c.evaluate("localStorage.removeItem('eema:auth')");
  const evidence=JSON.stringify({result:'PASS',checks,provider:'Mocked generateContent only; real parser, ranker, PostgreSQL and Spring HTTP',network:c.events.filter(e=>e.path==='/api/match'||e.path==='/api/venues/'+config.venueId)},null,2);for(const secret of secrets)assert(!evidence.includes(secret));fs.writeFileSync(path.join(out,'browser.json'),evidence+'\n');
 } catch(error) {
   if(c) {try {await c.screen('harness-diagnostic'); console.error('Browser location: '+await c.evaluate('location.href'));}catch{}}
   throw error;
 } finally {
   if(c){try{await c.send('Browser.close')}catch{}c.ws.close();}
   if(chrome && chrome.exitCode===null){const exited=new Promise(resolve=>chrome.once('exit',resolve));chrome.kill();await exited;}
   fs.rmSync(profile,{recursive:true,force:true,maxRetries:10,retryDelay:100});
 }
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',checks,error:e.message},null,2));console.error(e.message);process.exitCode=1;});
