#!/usr/bin/env node
// Reproduces the two discovered defects only against guarded local QA fixtures.
const cp=require('child_process'),crypto=require('crypto'),fs=require('fs'),path=require('path');
if(process.argv[2]!=='--local-qa')throw Error('Explicit --local-qa required');
const root=path.resolve(__dirname,'../..');cp.execFileSync(path.join(root,'scripts/reset-local-qa.sh'),['--dry-run'],{cwd:root,stdio:'pipe'});
const started=new Date().toISOString(),secrets=[];
const sql=q=>cp.execFileSync('docker',['compose','exec','-T','postgres','psql','-X','-U','eema','-d','eema','-At','-v','ON_ERROR_STOP=1'],{cwd:root,input:q,encoding:'utf8',stdio:['pipe','pipe','pipe']});
async function call(route,body,token){const r=await fetch('http://localhost:8080/api'+route,{method:body?'POST':'GET',headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});const value=await r.json();if(value.accessToken)secrets.push(value.accessToken,value.refreshToken);return {status:r.status,value};}
(async()=>{
 const email='auth-defect+'+crypto.randomUUID()+'@example.test';const password='é'.repeat(72),different='é'.repeat(36)+'x'.repeat(36);secrets.push(password,different);
 const registered=await call('/auth/register',{email,fullName:'QA Password Defect',role:'SCOUT',password});if(registered.status!==201)throw Error('Registration setup failed');
 const login=await call('/auth/login',{email,password:different});
 const adminEmail='auth-admin-probe+'+crypto.randomUUID()+'@example.test',fixturePassword='QaFixture-2026!';secrets.push(fixturePassword);
 const admin=await call('/auth/register',{email:adminEmail,fullName:'QA Admin Probe',role:'SCOUT',password:fixturePassword});if(admin.status!==201)throw Error('Admin fixture setup failed');const id=admin.value.user.id;
 if(!/^[0-9a-f-]{36}$/.test(id))throw Error('Invalid fixture ID');let search;
 try{sql(`UPDATE users SET role='ADMIN' WHERE id='${id}' AND email='${adminEmail}';`);search=await call('/admin/bookings',null,admin.value.accessToken);}
 finally{sql(`UPDATE users SET role='SCOUT',active=false WHERE id='${id}' AND email='${adminEmail}';`);}
 const logs=cp.execFileSync('docker',['compose','logs','--since',started,'--no-color','backend'],{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024});
 const secretFound=secrets.some(v=>logs.includes(v));
 const evidence={date:started,checks:[{id:'AUTH-D01',scenario:'AUTH-004',expectedDifferentPasswordLogin:401,actualDifferentPasswordLogin:login.status,registerStatus:registered.status,characterLength:72,registeredUtf8Bytes:144,alternateUtf8Bytes:108,sharedPrefixBytes:72,fixtureEmail:email,result:login.status===401?'PASS':'FAIL'},
 {id:'AUTH-D02',scenario:'Admin booking search; discovered during AUTH-006',expected:200,actual:search.status,safeMessage:search.value.message,temporaryAdminDemotedAndDeactivated:true,result:search.status===200?'PASS':'FAIL'}],rawSecretsFoundInLogs:secretFound};
 fs.writeFileSync(path.join(root,'docs/qa/evidence/auth/defects.json'),JSON.stringify(evidence,null,2)+'\n');console.log(JSON.stringify({checks:evidence.checks.map(x=>({id:x.id,result:x.result})),rawSecretsFoundInLogs:secretFound}));if(secretFound||evidence.checks.some(x=>x.result==='FAIL'))process.exitCode=1;
})().catch(e=>{console.error(e.message);process.exit(1)});
