#!/usr/bin/env node
// Existing AUTH CDP/ws harness, isolated policy database only. No external providers.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),cp=require('child_process'),WS=require('ws');
if(process.argv[2]!=='--local-qa')throw Error('Use --local-qa');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'docs/qa/evidence/policy');fs.mkdirSync(out,{recursive:true});
const secrets=[],checks=[],run='QA-'+Date.now(),password='QaFixture-2026!';
const types=['TERMS_OF_SERVICE','PRIVACY_POLICY','ACCEPTABLE_USE_POLICY','PRODUCTION_POLICY','INSURANCE_REQUIREMENTS'];
const content='QA TEST CONTENT — NOT LEGAL TERMS\n<b>exact plain text & no HTML</b>\n';
const compose=['compose','-p','eema-policy-qa','-f',path.join(root,'scripts/qa/policy-compose.yml')];
function sql(query){return cp.execFileSync('docker',[...compose,'exec','-T','postgres','psql','-X','-U','eema','-d','eema_policy_qa','-At','-v','ON_ERROR_STOP=1'],{input:query,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();}
function record(name,detail={}){checks.push({name,result:'PASS',...detail});}
async function api(route,body,status,account){const r=await fetch('http://localhost:8080/api'+route,{method:body?'POST':'GET',headers:{...(body?{'Content-Type':'application/json'}:{}),...(account?{Authorization:'Bearer '+account.accessToken}:{})},...(body?{body:JSON.stringify(body)}:{})});assert.equal(r.status,status,route);return r.json();}
async function connect(tab){const ws=new WS(tab.webSocketDebuggerUrl);await new Promise(r=>ws.on('open',r));let seq=0;const pending=new Map(),methods=new Map(),events=[],paused=[];
 ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error('CDP request failed')):p.resolve(m.result);}else if(m.method==='Network.requestWillBeSent')methods.set(m.params.requestId,m.params.request.method);else if(m.method==='Network.responseReceived'){let r=m.params.response;events.push({path:new URL(r.url).pathname,method:methods.get(m.params.requestId),status:r.status});}else if(m.method==='Fetch.requestPaused')paused.push(m.params.requestId);});
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error('Browser evaluation failed');return r.result.value;};
 async function wait(expression,label){for(let i=0;i<150;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100));}throw Error('Browser timeout: '+label);}
 async function go(route){const target='http://localhost:3000'+route;let result;for(let attempt=0;attempt<3;attempt++){result=await send('Page.navigate',{url:target});if(result.errorText!=='net::ERR_ABORTED')break;await new Promise(r=>setTimeout(r,250));}if(result.errorText)throw Error('Navigation failed: '+result.errorText);await wait('location.href==='+JSON.stringify(target)+' && document.readyState==="complete"','document ready');}
 async function click(text){await evaluate(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!b)throw Error('button missing');b.click();})()`);}
 async function fill(values){await evaluate(`(()=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;document.querySelectorAll('form input').forEach((el,i)=>{set.call(el,${JSON.stringify(values)}[i]);el.dispatchEvent(new Event('input',{bubbles:true}));});})()`);}
 const session=async()=>{const s=await evaluate("JSON.parse(localStorage.getItem('eema:auth'))");if(s)secrets.push(s.accessToken,s.refreshToken);return s;};
 const screen=async name=>{const metrics=await send('Page.getLayoutMetrics');const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true,clip:{x:0,y:0,width:metrics.cssContentSize.width,height:Math.min(metrics.cssContentSize.height,12000),scale:1}});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));};
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});await send('Network.enable');return {send,evaluate,wait,go,click,fill,session,screen,events,paused,ws};
}
(async()=>{
 assert.equal(sql('select current_database()'),'eema_policy_qa');
 assert.equal((await api('/policies/current',null,401)).error,'Unauthorized');
 const accounts={};for(const role of ['SCOUT','OWNER','ADMIN']){accounts[role]=await api('/auth/register',{email:run+'-'+role+'@example.test',fullName:'QA Policy '+role,password,role:role==='ADMIN'?'SCOUT':role},201);sql(`update users set email_verified=true${role==='ADMIN'?",role='ADMIN'":''} where id='${accounts[role].user.id}';`);accounts[role]=await api('/auth/login',{email:accounts[role].user.email,password},200);}
 sql(`insert into owner_verifications(owner_id,status,reviewed_at,reviewed_by,verification_method) values('${accounts.OWNER.user.id}','VERIFIED',now(),'${accounts.ADMIN.user.id}','MANUAL_REVIEW');`);
 const c=await connect((await(await fetch('http://localhost:9223/json')).json())[0]);
 async function login(role,route='/policies'){await c.go('/');await c.evaluate('localStorage.setItem("eema:auth",'+JSON.stringify(JSON.stringify(accounts[role]))+')');await c.go(route);}
 async function ready(){await c.wait("document.body.textContent.includes('Eema policy centre') && !document.body.textContent.includes('Loading current policies')",'policy centre');}
 async function publish(type,suffix){const p=await api('/admin/policies',{policyType:type,version:run+'-'+suffix,title:'QA TEST CONTENT — NOT LEGAL TERMS '+type,effectiveAt:'2099-01-01T00:00:00Z',content},201,accounts.ADMIN);return api('/admin/policies/'+p.id+'/publish',{},200,accounts.ADMIN);}
 let current=await api('/policies/current',null,200,accounts.SCOUT);
 for(const p of current){assert.ok(p.content.startsWith('QA TEST CONTENT — NOT LEGAL TERMS'));await api('/admin/policies/'+p.id+'/retire',{},200,accounts.ADMIN);}
 await login('SCOUT');await ready();await c.wait("document.body.textContent.includes('Policies are not configured')",'empty policies');await c.screen('empty');record('Empty published set: no injected text');
 const initial=[];for(const t of types.slice(0,4))initial.push(await publish(t,'v1-'+t));
 await api('/policies/accept',{policyVersionIds:initial.map(p=>p.id)},200,accounts.SCOUT);
 await login('SCOUT');await ready();
 const falseComplete=await c.evaluate("document.body.textContent.includes('All current required policies reviewed')");
 const authoritative=await api('/policies/status',null,200,accounts.SCOUT);assert.equal(authoritative.allRequiredAccepted,false);
 if(process.argv.includes('--probe')){await c.screen('before-missing-type');fs.writeFileSync(path.join(out,'ui-defect-before.json'),JSON.stringify({result:falseComplete?'REPRODUCED':'NOT_REPRODUCED',backendComplete:false,uiClaimsComplete:falseComplete},null,2));await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');return;}
 assert.equal(falseComplete,false);await c.wait("document.body.textContent.includes('required policy set')",'missing set notice');record('Missing required type cannot claim completion');
 initial.push(await publish(types[4],'v1-'+types[4]));
 await login('OWNER');await ready();await c.wait("document.querySelectorAll('input[type=checkbox]').length===5",'five controls');
 assert.equal(await c.evaluate("document.querySelectorAll('input:checked').length"),0);
 assert.equal(await c.evaluate("Array.from(document.querySelectorAll('article')).every(e=>e.textContent.includes("+JSON.stringify(content)+"))"),true);
 assert.equal(await c.evaluate("document.querySelectorAll('article b').length"),0);
 assert.equal(await c.evaluate("document.body.textContent.includes('I acknowledge that I have reviewed this privacy notice.')"),true);
 await c.evaluate("document.querySelector('input[type=checkbox]').click()");assert.equal(await c.evaluate("Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='Accept selected current policies').disabled"),true);
 record('OWNER exact plain text, explicit unchecked privacy acknowledgement; UI subset selection does not submit');
 await c.evaluate("document.querySelectorAll('input[type=checkbox]:not(:checked)').forEach(e=>e.click())");await c.click('Accept selected current policies');await c.wait("document.body.textContent.includes('All current required policies reviewed')",'owner accepted');await c.screen('owner-accepted');
 await login('SCOUT');await ready();await c.wait("document.querySelectorAll('input[type=checkbox]').length===1",'persisted subset');record('SCOUT persisted subset shown as reviewed; remaining control unchecked');
 await c.evaluate("document.querySelector('input[type=checkbox]').click()");await c.click('Accept selected current policies');await c.wait("document.body.textContent.includes('All current required policies reviewed')",'scout all');
 await login('OWNER','/owner/payout-destination');await c.wait("document.body.textContent.includes('Add payout details')",'eligible owner');await c.click('Add payout details');await c.wait("!!document.querySelector('#iban')",'owner form before replacement');
 const next=await publish(types[0],'v2');await c.fill(['QA Owner','NL91ABNA0417164300']);await c.click('Submit for verification');await c.wait("document.body.textContent.includes('Review updated policies')",'403 stale refresh gate');assert.equal(c.events.filter(e=>e.path==='/api/owner/payout-destinations'&&e.method==='POST'&&e.status===403).length,1);await c.screen('owner-gated');record('OWNER stale form submits once: backend 403 triggers global policy refresh and gate');
 await login('SCOUT');await ready();await c.wait("document.querySelectorAll('input[type=checkbox]').length===1",'replacement');await c.screen('scout-replacement');record('SCOUT replacement requires explicit review on revisiting policy centre');
 await c.evaluate("document.querySelector('input[type=checkbox]').click()");await c.click('Accept selected current policies');await c.wait("document.body.textContent.includes('All current required policies reviewed')",'replacement accepted');
 await login('OWNER');await ready();await c.wait("document.querySelectorAll('input[type=checkbox]').length===1",'owner replacement');await c.evaluate("document.querySelector('input[type=checkbox]').click()");await c.click('Accept selected current policies');await c.wait("document.body.textContent.includes('All current required policies reviewed')",'owner reaccepted');await c.go('/owner/payout-destination');await c.wait("document.body.textContent.includes('Add payout details')",'owner add');await c.click('Add payout details');await c.wait("!!document.querySelector('#iban')",'owner form');await c.fill(['QA Owner','NL91ABNA0417164300']);await c.click('Submit for verification');await c.wait("document.body.textContent.includes('Payout details under review')",'owner submission after acceptance');record('OWNER previously gated submission succeeds after reviewing replacement');
 await login('ADMIN','/admin/policies');await c.wait("document.body.textContent.includes('Create policy version')",'admin policies');
 async function createUi(suffix){await c.click('Create policy version');await c.wait("!!document.querySelector('textarea')",'draft form');await c.fill([run+'-'+suffix,'QA TEST CONTENT — NOT LEGAL TERMS UI','2099-01-01T12:00']);await c.evaluate('(()=>{const e=document.querySelector("textarea");Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,"value").set.call(e,'+JSON.stringify(content)+');e.dispatchEvent(new Event("input",{bubbles:true}));})()');await c.click('Create draft');await c.wait('document.body.textContent.includes('+JSON.stringify(run+'-'+suffix)+') && !document.querySelector("textarea")','draft saved');}
 let confirmations=0;c.ws.on('message',raw=>{const m=JSON.parse(raw);if(m.method==='Page.javascriptDialogOpening'){confirmations++;void c.send('Page.handleJavaScriptDialog',{accept:true});}});
 async function rowAction(suffix,label){await c.evaluate('(()=>{const row=Array.from(document.querySelectorAll("article")).find(e=>e.textContent.includes('+JSON.stringify(run+'-'+suffix)+'));row.querySelector("details").open=true;row.querySelector("button").click();})()');await c.wait('Array.from(document.querySelectorAll("article")).some(e=>e.textContent.includes('+JSON.stringify(run+'-'+suffix)+') && e.textContent.includes('+JSON.stringify(label)+'))','admin action result');}
 await createUi('ui-v1');await rowAction('ui-v1','PUBLISHED');await createUi('ui-v2');await rowAction('ui-v2','PUBLISHED');await rowAction('ui-v2','RETIRED');
 await c.send('Page.reload');await c.wait('document.body.textContent.includes('+JSON.stringify(run+'-ui-v2')+')','admin reload');
 assert.equal(await c.evaluate("document.querySelectorAll('article input,article textarea,article select').length"),0);
 assert.equal(confirmations,3);await c.screen('admin-history');record('ADMIN UI creates drafts, publishes/replaces/retires with confirmation and retains read-only history');
 await c.evaluate("localStorage.removeItem('eema:auth');sessionStorage.clear()");await c.send('Browser.close');fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'PASS',run,checks,legal:'LEGAL REVIEW REQUIRED'},null,2)+'\n');
})().catch(e=>{fs.writeFileSync(path.join(out,'browser.json'),JSON.stringify({result:'FAIL',checks,error:e.message},null,2));console.error(e.message);process.exit(1)});
