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
 const deferred=()=>{let resolve;const promise=new Promise(r=>resolve=r);return {promise,resolve}};
 for(const endpoint of ['/match','/venues/fixture','/bookings/fixture/checkout','/admin/payouts/fixture/mark-paid','/bookings/fixture/receipt']){
  for(const switched of [false,true]){
   const entered=deferred(),release=deferred();let originals=0,refreshes=0,newActorCalls=0;
   const h=harness(async(url,options)=>{if(url.endsWith('/auth/refresh')){refreshes++;entered.resolve();await release.promise;return response(200,sample())}originals++;if(options.headers.get('Authorization')==='Bearer actor-b')newActorCalls++;return endpoint.endsWith('/receipt')&&originals>1?new Response('%PDF-synthetic',{status:200,headers:{'Content-Type':'application/pdf'}}):response(originals===1?401:200,{ok:true})});
   const pending=(endpoint.endsWith('/receipt')?h.client.apiBinaryRequest(endpoint):h.client.apiRequest(endpoint,{auth:'required',method:endpoint.endsWith('/receipt')?'GET':endpoint.includes('/venues/')?'PUT':'POST',body:endpoint.endsWith('/receipt')?undefined:{synthetic:true}})).then(()=>200,e=>e.status);
   await entered.promise;if(switched)h.token.saveAuthSession({...sample(),accessToken:'actor-b',user:{...sample().user,id:'actor-b'}});release.resolve();const status=await pending;
   const passed=switched?status===401&&originals===1&&newActorCalls===0:status===200&&originals===2&&refreshes===1;
   results.push({endpoint,variant:switched?'different actor during refresh':'same actor refresh',result:passed?'PASS':'FAIL',status,originals,refreshes,newActorCalls});
  }
 }
 for(const endpoint of ['/bookings','/availability-inquiries','/bookings/fixture/accept','/bookings/fixture/decline','/bookings/fixture/checkout','/bookings/fixture/cancel','/bookings/fixture/incidents','/policies/accept','/venues/fixture/submit-for-review','/owner/verification/submit','/owner/payout-destinations','/admin/payout-destinations/fixture/verify','/admin/payout-destinations/fixture/reject','/admin/payouts/fixture/mark-paid','/admin/liabilities/fixture/settle']){
  let count=0;const h=harness(async()=>{count++;throw Error('Synthetic response lost')});let status;
  try{await h.client.apiRequest(endpoint,{auth:'required',method:'POST',body:{synthetic:true}})}catch(e){status=e.code}
  assert.equal(count,1);assert.equal(status,'NETWORK');results.push({endpoint,variant:'unknown transport outcome is never automatically replayed',result:'PASS',calls:count});
 }
 for(const endpoint of ['/match','/venues/fixture','/bookings/fixture/checkout','/admin/payouts/fixture/mark-paid','/bookings/fixture/receipt']){
  let refresh=0,calls=0;const h=harness(async url=>{if(url.endsWith('/auth/refresh'))refresh++;else calls++;return response(401,{})});
  await assert.rejects(h.client.apiRequest(endpoint,{auth:'required',method:endpoint.endsWith('/receipt')?'GET':'POST'}),e=>e.status===401);
  assert.equal(refresh,1);assert.equal(calls,1);assert.equal(h.token.getAuthSession(),null);results.push({endpoint,variant:'failed refresh clears session without mutation replay',result:'PASS',calls,refresh});
  h.token.saveAuthSession(sample());assert(h.token.getAuthSession());
 }
 console.log(JSON.stringify({result:results.every(x=>x.result==='PASS')?'PASS':'FAIL',checks:results},null,2));
 if(results.some(x=>x.result==='FAIL'))process.exitCode=1;
})().catch(e=>{console.error(e.message);process.exitCode=1});
