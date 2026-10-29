#!/usr/bin/env node
const fs=require('fs'),vm=require('vm'),assert=require('assert/strict'),path=require('path');
const frontend=path.resolve(__dirname,'../../../Eema-frontend');
const ts=require(path.join(frontend,'node_modules/typescript'));
let session={accessToken:'QA_TOKEN_ONLY',user:{id:'qa',emailVerified:false}},requests=[];
const code=ts.transpileModule(fs.readFileSync(path.join(frontend,'lib/api/client.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
const moduleExports={};
const context={exports:moduleExports,require:name=>name==='@/lib/config'?{API_BASE_URL:'http://qa.invalid/api'}:{getAuthSession:()=>session},Headers,FormData,fetch:async(url,options)=>{requests.push({url,method:options.method});return new Response('{}',{status:200,headers:{'Content-Type':'application/json'}})},console};
vm.runInNewContext(code,context);
(async()=>{
 for(const role of ['SCOUT','OWNER','ADMIN']){session.user.role=role;await moduleExports.apiRequest('/match',{method:'POST',auth:'required',body:{brief:'QA discovery fixture'}});}
 assert.equal(requests.length,3);
 for(const route of ['/bookings','/venues','/owner/verification/submit'])await assert.rejects(moduleExports.apiRequest(route,{method:'POST',auth:'required',body:{}}),e=>e.status===403);
 assert.equal(requests.length,3);
 session=null;await assert.rejects(moduleExports.apiRequest('/match',{method:'POST',auth:'required'}),e=>e.status===401);assert.equal(requests.length,3);
 const result={result:'PASS',checks:7,scope:'Three authenticated roles can search unverified; three unrelated mutation guards retained; anonymous client denied'};
 fs.writeFileSync(path.resolve(__dirname,'../../docs/qa/evidence/match/client.json'),JSON.stringify(result,null,2)+'\n');console.log('PASS matching client checks: 7');
})().catch(e=>{console.error(e.message);process.exitCode=1});
