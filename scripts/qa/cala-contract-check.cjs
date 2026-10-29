// Executes actual Next route and Cala client, with deterministic synthetic transport only.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert/strict');
const baseline=process.argv.includes('--baseline');
const front=path.resolve(__dirname,'../../../Eema-frontend');const ts=require(front+'/node_modules/typescript');const next=require(front+'/node_modules/next/server');
function harness(key,reply){let timer;const calls=[],logs=[];const cache={};const context=vm.createContext({process:{env:{CALA_API_KEY:key}},AbortController,Response,console:{error:(...x)=>logs.push(x.join(' '))},setTimeout:(fn,ms)=>{assert.equal(ms,6000);timer=fn;return 1},clearTimeout:()=>{timer=null},fetch:async(url,opts)=>{calls.push({url,key:opts.headers['X-API-KEY']});return reply(calls.length,url,opts,()=>timer?.())}});
 function load(name){if(name==='next/server')return next;if(cache[name])return cache[name];const rel=name.replace('@/','');const text=baseline?require('child_process').execFileSync('git',['show','HEAD:'+rel+'.ts'],{cwd:front,encoding:'utf8'}):fs.readFileSync(front+'/'+rel+'.ts','utf8');const js=ts.transpileModule(text,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;const exports={};cache[name]=exports;vm.runInContext('(function(require,exports){'+js+'\n})',context)(load,exports);return exports}return {route:load('@/app/api/verify-booker/route'),calls,logs};}
const json=(x,status=200)=>new Response(JSON.stringify(x),{status,headers:{'content-type':'application/json'}});
const hit={id:'fixture-company',name:'Different Synthetic Company',entity_type:'Company',unneededMarker:'SYNTHETIC_RAW_MARKER'};
const cases=[
 ['JSON number','123','synthetic-key',()=>{throw Error('must not call')},'false'],
 ['JSON array','[]','synthetic-key',()=>{throw Error('must not call')},'false'],
 ['numeric company',JSON.stringify({company:123}),'synthetic-key',()=>{throw Error('must not call')},'false'],
 ['quoted blank key',JSON.stringify({company:'Synthetic'}),'" "',()=>{throw Error('must not call')},'false'],
 ['malformed JSON','{','synthetic-key',()=>{throw Error('must not call')},'false'],
 ['empty object','{}','synthetic-key',()=>{throw Error('must not call')},'false'],
 ['blank company',JSON.stringify({company:'  '}),'synthetic-key',()=>{throw Error('must not call')},'false'],
 ['missing key',JSON.stringify({company:'Synthetic'}),'',()=>{throw Error('must not call')},'false'],
 ['quoted key and success',JSON.stringify({company:'Synthetic'}),' "synthetic-key" ',n=>json(n===1?{entities:[hit]}:{...hit,properties:{lei:{value:'QA-ID',sources:['test']}}}),'true'],
 ['no company match',JSON.stringify({company:'Synthetic'}),'synthetic-key',()=>json({entities:[]}),'false'],
 ['detail failure fallback',JSON.stringify({company:'Synthetic'}),'synthetic-key',n=>n===1?json({entities:[hit]}):json({message:'Synthetic'},500),'true'],
 ['provider 500',JSON.stringify({company:'Synthetic'}),'synthetic-key',()=>json({},500),'false'],
 ['timeout',JSON.stringify({company:'Synthetic'}),'synthetic-key',(n,u,o,abort)=>{abort();assert.equal(o.signal.aborted,true);throw Error('Synthetic abort')},'false'],
 ['wrong search shape',JSON.stringify({company:'Synthetic'}),'synthetic-key',()=>json({entities:{unexpected:true}}),'false'],
 ['null request','null','synthetic-key',()=>{throw Error('must not call')},'false'],
 ['non-string request',JSON.stringify({company:{toString:null}}),'synthetic-key',()=>{throw Error('must not call')},'false'],
 ['anonymous unrelated match and raw data',JSON.stringify({company:'No Matching Synthetic Company'}),'synthetic-key',n=>json(n===1?{entities:[hit]}:{...hit,privateMarker:'SYNTHETIC_RAW_MARKER'}),'true'],
 ['provider error log echo',JSON.stringify({company:'Synthetic'}),'synthetic-key',()=>json({debug:'SYNTHETIC_SENSITIVE_MARKER'},500),'false']
];
(async()=>{const results=[];for(const [name,body,key,reply,expected]of cases){const h=harness(key,reply);let response,result,error;try{response=await h.route.POST(new next.NextRequest('http://localhost/api/verify-booker',{method:'POST',body,headers:{'content-type':'application/json'}}));result=await response.json()}catch(e){error=e.name}
 const expectedCalls={'quoted key and success':2,'no company match':1,'detail failure fallback':3,'provider 500':2,'timeout':1,'wrong search shape':1,'anonymous unrelated match and raw data':2,'provider error log echo':2}[name]??0;
 const r={name,expectedCalls,httpStatus:response?.status??null,verified:result?.verified??null,providerCalls:h.calls.length,rawReturned:JSON.stringify(result??{}).includes('SYNTHETIC_RAW_MARKER'),sensitiveMarkerLogged:h.logs.some(x=>x.includes('SYNTHETIC_SENSITIVE_MARKER')||x.includes('Synthetic')),keyNormalized:h.calls.every(c=>c.key==='synthetic-key'),error:error??null};r.result=h.calls.length===expectedCalls&&!error&&response.status===200&&String(result.verified)===expected&&!r.rawReturned&&!r.sensitiveMarkerLogged&&r.keyNormalized?'PASS':'FAIL';results.push(r)}
fs.writeFileSync('docs/qa/evidence/cala/'+(baseline?'contract-baseline.json':'contract.json'),JSON.stringify({scope:'Actual NextRequest/NextResponse and source modules; deterministic mock transport/timer, no real provider',results},null,2)+'\n');console.log(JSON.stringify(results));if(results.some(x=>x.result==='FAIL'))process.exitCode=1;})().catch(e=>{console.error(e.message);process.exitCode=1});
