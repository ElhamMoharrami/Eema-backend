#!/usr/bin/env node
// Execute the actual session parser with controlled browser storage; save names/results only.
const fs=require('fs'),path=require('path'),vm=require('vm'),crypto=require('crypto');
const root=path.resolve(__dirname,'../../../Eema-frontend'),ts=require(path.join(root,'node_modules/typescript'));
const js=ts.transpileModule(fs.readFileSync(path.join(root,'lib/auth/token.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
let stored;const context=vm.createContext({crypto,window:{localStorage:{getItem:()=>stored,setItem:(_,v)=>stored=v,removeItem:()=>stored=null}}});
const exportsObject={};vm.runInContext('(function(exports){'+js+'\n})',context)(exportsObject);
const valid=()=>({accessToken:'qa-access',refreshToken:'qa-refresh',expiresIn:900,storedAt:123,generation:'qa-generation',user:{id:'qa-user',email:'qa@example.test',fullName:'QA',role:'SCOUT',active:true,emailVerified:true,phone:null}});
const checks=[];function check(name,fn){let pass=false;try{pass=!!fn()}catch{}checks.push({name,result:pass?'PASS':'FAIL'});}
for(const [name,value] of [['invalid JSON','{'],['null','null'],['array','[]'],['empty','{}']]){stored=value;check(name,()=>exportsObject.getAuthSession()===null);}
const variants={
 'missing role':s=>delete s.user.role,'invalid role':s=>s.user.role='ROOT','missing name':s=>delete s.user.fullName,'object name':s=>s.user.fullName={},
 'missing id':s=>delete s.user.id,'number email':s=>s.user.email=123,'string user':s=>s.user='ADMIN','array user':s=>s.user=[],
 'object access token':s=>s.accessToken={},'blank access token':s=>s.accessToken=' ','number refresh token':s=>s.refreshToken=42,
 'missing expiry':s=>delete s.expiresIn,'negative expiry':s=>s.expiresIn=-1,'string expiry':s=>s.expiresIn='900',
 'missing active':s=>delete s.user.active,'string verified':s=>s.user.emailVerified='true','missing verified':s=>delete s.user.emailVerified,
 'bad generation':s=>s.generation={},'bad timestamp':s=>s.storedAt='yesterday'};
for(const [name,change] of Object.entries(variants)){const s=valid();change(s);stored=JSON.stringify(s);check(name,()=>exportsObject.getAuthSession()===null);}
stored=JSON.stringify(valid());check('valid current session retained',()=>exportsObject.getAuthSession()?.generation==='qa-generation');
const legacy=valid();delete legacy.generation;stored=JSON.stringify(legacy);check('valid legacy session retained',()=>exportsObject.getAuthSession()?.generation==='legacy:123');
context.window.localStorage.getItem=()=>{throw Error('storage unavailable')};check('unavailable storage fails closed',()=>exportsObject.getAuthSession()===null);
const result=checks.every(x=>x.result==='PASS')?'PASS':'FAIL',out=path.resolve(__dirname,'../../docs/qa/evidence/sec/client.json');fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify({result,checks},null,2)+'\n');console.log(result,checks.filter(c=>c.result==='PASS').length+'/'+checks.length);if(result!=='PASS')process.exitCode=1;
