#!/usr/bin/env node
// Execute the actual hook with a minimal deterministic hook host; no transport timing sleeps.
const fs=require('fs'),vm=require('vm'),path=require('path'),assert=require('assert/strict');
const frontend=path.resolve(__dirname,'../../../Eema-frontend'),ts=require(path.join(frontend,'node_modules/typescript'));
const values=[],refs=[];let cursor=0,refCursor=0;
const React={useState(initial){const i=cursor++;if(!(i in values))values[i]=initial;return [values[i],v=>{values[i]=typeof v==='function'?v(values[i]):v}];},useRef(initial){const i=refCursor++;if(!(i in refs))refs[i]={current:initial};return refs[i];},useCallback:f=>f,useEffect:()=>{}};
class ApiError extends Error{}
const source=fs.readFileSync(path.join(frontend,'hooks/useAdminPage.ts'),'utf8'),js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText,exportsObject={};
vm.runInNewContext('(function(require,exports){'+js+'\n})')((name)=>name==='react'?React:name.includes('error-message')?{apiErrorMessage:e=>e.message}:{ApiError},exportsObject);
(async()=>{let releaseOld,releaseNew;const old=new Promise(r=>releaseOld=r),newer=new Promise(r=>releaseNew=r);let n=0;const hook=exportsObject.useAdminPage(()=>++n===1?old:newer);const a=hook.load(),b=hook.load();releaseNew({id:'new-record'});await b;releaseOld({id:'stale-record'});await a;assert.equal(values[0].id,'new-record','An older admin load must not overwrite the newer authoritative result');console.log(JSON.stringify({result:'PASS',check:'Out-of-order admin requests retain latest result'}));})().catch(e=>{console.error(JSON.stringify({result:'FAIL',error:e.message}));process.exitCode=1;});
