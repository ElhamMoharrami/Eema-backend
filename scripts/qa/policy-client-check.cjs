#!/usr/bin/env node
// Executes the real TypeScript client/token modules with controlled transport and tab contexts.
const fs=require('fs'),vm=require('vm'),path=require('path'),assert=require('assert/strict');
const frontend=path.resolve(__dirname,'../../../Eema-frontend');
const ts=require(path.join(frontend,'node_modules/typescript'));
const results=[];
const sample=()=>({accessToken:'fixture-access',refreshToken:'fixture-refresh',tokenType:'Bearer',expiresIn:900,user:{id:'fixture-user',email:'qa@example.test',fullName:'QA Auth',role:'SCOUT',active:true,emailVerified:true}});
function harness(fetch,shared=new Map()){
 const dispatched=[];const window={localStorage:{getItem:k=>shared.get(k)||null,setItem:(k,v)=>shared.set(k,v),removeItem:k=>shared.delete(k)},dispatchEvent:e=>dispatched.push(e.type)};
 const context=vm.createContext({crypto:require("crypto").webcrypto,window,fetch,Headers,Response,FormData,Event,setTimeout,clearTimeout,console:{log:()=>{}}});const cache={};
 function load(name){if(cache[name])return cache[name];if(name==='@/lib/config')return {API_BASE_URL:'http://qa/api'};
  const rel=name.replace('@/','');const source=fs.readFileSync(path.join(frontend,rel+'.ts'),'utf8');
  const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
  const exports={};cache[name]=exports;const fn=vm.runInContext('(function(require,exports){'+js+'\n})',context);fn(child=>load(child.startsWith(".")?path.posix.normalize(path.posix.dirname(name)+"/"+child):child),exports);return exports;
 }
 const token=load('@/lib/auth/token'),client=load('@/lib/api/client');token.saveAuthSession(sample());return {token,client,shared,dispatched,load};
}
const response=(status,body)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
(async()=>{
 const checks=[];
 for(let repeat=0;repeat<3;repeat++)for(const variant of ['policy','ownership']){
   let refresh=0,mutations=0;
   const h=harness(async url=>{if(url.endsWith('/auth/me'))return response(200,sample().user);if(url.endsWith('/auth/refresh')){refresh++;return response(200,sample());}mutations++;return response(403,{message:variant==='policy'?'Current required policies must be accepted before this action':'Only the venue owner can submit this venue'});});
   let caught;try{await h.client.apiRequest('/venues/fixture/submit-for-review',{auth:'required',method:'POST'});}catch(e){caught=e;}
   assert.equal(caught.status,403);assert.equal(refresh,0);assert.equal(mutations,1);
   assert.equal(h.dispatched.filter(x=>x==='eema:policy-stale').length,variant==='policy'?1:0);
   const text=h.load('@/lib/api/error-message').apiErrorMessage(caught);
   if(variant==='policy')assert.match(text,/review the current versions/);
   checks.push({repeat,variant,result:'PASS',mutations,refresh,policyStaleEvents:variant==='policy'?1:0});
 }
 console.log(JSON.stringify({result:'PASS',source:'Actual frontend client, auth and error-message modules; controlled transport',checks},null,2));
})().catch(e=>{console.error(e.message);process.exit(1)});
