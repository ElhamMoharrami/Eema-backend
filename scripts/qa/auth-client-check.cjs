#!/usr/bin/env node
// Executes the real TypeScript client/token modules with controlled transport and tab contexts.
const fs=require('fs'),vm=require('vm'),path=require('path'),assert=require('assert/strict');
const frontend=path.resolve(__dirname,'../../../Eema-frontend');
const ts=require(path.join(frontend,'node_modules/typescript'));
const results=[];
const sample=()=>({accessToken:'fixture-access',refreshToken:'fixture-refresh',tokenType:'Bearer',expiresIn:900,user:{id:'fixture-user',email:'qa@example.test',fullName:'QA Auth',role:'SCOUT',active:true,emailVerified:true}});
function harness(fetch,shared=new Map()){
 const window={localStorage:{getItem:k=>shared.get(k)||null,setItem:(k,v)=>shared.set(k,v),removeItem:k=>shared.delete(k)},dispatchEvent:()=>{}};
 const context=vm.createContext({crypto:require("crypto").webcrypto,window,fetch,Headers,Response,FormData,Event,setTimeout,clearTimeout,console:{log:()=>{}}});const cache={};
 function load(name){if(cache[name])return cache[name];if(name==='@/lib/config')return {API_BASE_URL:'http://qa/api'};
  const rel=name.replace('@/','');const source=fs.readFileSync(path.join(frontend,rel+'.ts'),'utf8');
  const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
  const exports={};cache[name]=exports;const fn=vm.runInContext('(function(require,exports){'+js+'\n})',context);fn(load,exports);return exports;
 }
 const token=load('@/lib/auth/token'),client=load('@/lib/api/client');token.saveAuthSession(sample());return {token,client,shared};
}
const response=(status,body)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
(async()=>{
 for(const variant of ['valid','second401','refresh401','network','malformed','missing-user','missing-role']){
  let refresh=0,original=0;
  const h=harness(async url=>{
   if(url.endsWith('/auth/refresh')){refresh++;if(variant==='network')throw Error('offline');if(variant==='refresh401')return response(401,{});if(variant==='malformed')return new Response('{',{status:200});let s=sample();if(variant==='missing-user')delete s.user;if(variant==='missing-role')delete s.user.role;return response(200,s);}
   original++;return response(original===1||variant==='second401'?401:200,{message:'Unauthorized',ok:true});
  });
  let status='success';try{await h.client.apiRequest('/probe',{auth:'required'});}catch(e){status=e.status;}
  assert.equal(refresh,1);assert.equal(original,['valid','second401'].includes(variant)?2:1);
  assert.equal(!!h.token.getAuthSession(),['valid','second401'].includes(variant));assert.equal(status,variant==='valid'?'success':401);
  results.push({variant,result:'PASS',refreshCalls:refresh,originalCalls:original,sessionRetained:!!h.token.getAuthSession()});
 }
 const deferred=()=>{let resolve;const promise=new Promise(r=>resolve=r);return {promise,resolve};};
 const entered=deferred(),release=deferred();let refresh=0,original=0;
 const h=harness(async url=>{if(url.endsWith('/auth/refresh')){refresh++;entered.resolve();await release.promise;return response(200,{...sample(),refreshToken:'rotated'});}original++;return response(original<=5?401:200,{});});
 const requests=Array.from({length:5},()=>h.client.apiRequest('/probe',{auth:'required'}));
 await entered.promise;release.resolve();await Promise.all(requests);assert.equal(refresh,1);assert.equal(original,10);
 results.push({variant:'same-tab-five-requests',result:'PASS',refreshCalls:refresh,originalCalls:original});
 const shared=new Map(),startedA=deferred(),startedB=deferred(),releaseA=deferred(),releaseB=deferred();
 let failNewer=false;
 function transport(which){let n=0;return async url=>{if(url.endsWith('/auth/refresh')){(which==='A'?startedA:startedB).resolve();await (which==='A'?releaseA:releaseB).promise;return which==='A'?response(200,{...sample(),refreshToken:'winner-refresh',accessToken:'winner-access'}):response(401,{});}return response(++n===1||failNewer?401:200,{});};}
 const a=harness(transport('A'),shared),b=harness(transport('B'),shared);let observed;
 b.token.subscribeAuthSession(s=>observed=s);
 const pa=a.client.apiRequest('/probe',{auth:'required'}),pb=b.client.apiRequest('/probe',{auth:'required'});
 await Promise.all([startedA.promise,startedB.promise]);releaseA.resolve();await pa;
 releaseB.resolve();await pb;assert.equal(b.token.getAuthSession().refreshToken,'winner-refresh');assert.equal(observed.user.role,'SCOUT');
 results.push({variant:'cross-tab-winner-then-loser',result:'PASS',refreshCalls:2,sharedSessionPreserved:true,loserSynchronized:true});
 // A genuine failure of that newer generation must still sign out.
 failNewer=true;await assert.rejects(b.client.apiRequest('/probe',{auth:'required'}),e=>e.status===401);assert.equal(shared.has('eema:auth'),false);assert.equal(observed,null);
 results.push({variant:'newer-generation-genuine-failure',result:'PASS',sessionCleared:true});
 // A newer session does not allow the original request to retry forever.
 const pending=deferred(),started=deferred();let originals=0,refreshes=0;
 const c=harness(async url=>{if(url.endsWith('/auth/refresh')){refreshes++;started.resolve();await pending.promise;return response(401,{});}originals++;return response(401,{});});
 const pc=c.client.apiRequest('/probe',{auth:'required'}).catch(e=>e.status);await started.promise;
 c.token.saveAuthSession({...sample(),refreshToken:'newer'});pending.resolve();assert.equal(await pc,401);assert.equal(originals,2);assert.equal(refreshes,1);assert.ok(c.token.getAuthSession());
 results.push({variant:'cross-tab-recovery-retries-once',result:'PASS',originalCalls:originals,refreshCalls:refreshes});
 for(const variant of ['logout-during-refresh','login-during-refresh']){
  const started=deferred(),release=deferred();let count=0;
  const tab=harness(async url=>{if(url.endsWith('/auth/refresh')){started.resolve();await release.promise;return response(200,{...sample(),refreshToken:'obsolete-response'});}return response(++count===1?401:200,{});});
  const request=tab.client.apiRequest('/probe',{auth:'required'}).catch(e=>e.status);await started.promise;
  if(variant==='logout-during-refresh')tab.token.clearAuthSession();else tab.token.saveAuthSession({...sample(),refreshToken:'new-login'});
  release.resolve();const result=await request;
  if(variant==='logout-during-refresh'){assert.equal(result,401);assert.equal(tab.token.getAuthSession(),null);}else assert.equal(tab.token.getAuthSession().refreshToken,'new-login');
  results.push({variant,result:'PASS',staleResponseIgnored:true});
 }
 console.log(JSON.stringify({result:'PASS',source:'Actual lib/api/client.ts and lib/auth/token.ts; simulated transport',checks:results},null,2));
})().catch(e=>{console.error(e.message);process.exit(1)});
