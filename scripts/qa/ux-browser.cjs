// Deterministic presentation/transport checks. API is intercepted; backend authority is proven in prior domain reports.
const {launch,out}=require('./ux-cdp.cjs'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
const expectedLoaders=require('./ux-loaders.json');const fixtures=require('./ux-fixtures.json'),inventory=require('../../docs/qa/evidence/ux/inventory.json');
const id=fixtures.User.id,checks=[],failures=[];
if(process.argv.includes('--long')){
 for(const value of Object.values(fixtures)){
  for(const key of ['name','venueName','title'])if(typeof value[key]==='string')value[key]='Å漢字'+ 'LongName'.repeat(14);
  for(const key of ['reason','description','message','ownerNote','note','rawBrief','content'])if(typeof value[key]==='string')value[key]='Long unbroken text: '+ '文'.repeat(1000);
  if('accountHolderName' in value)value.accountHolderName='É'+ 'Holder'.repeat(26);
  for(const key of ['hourlyRateEur','venuePriceEur','grossRetainedAmountEur','payoutAmountEur','amountEur'])if(key in value)value[key]=99999999.99;
 }
}

let role='SCOUT',empty=false,fault=null,held=[],hold=false,c,requests=[],unexpected=[];
const page=(item)=>({content:empty?[]:[item],number:0,totalPages:empty?0:1,totalElements:empty?0:1,size:20,numberOfElements:empty?0:1,first:true,last:true,empty});
const list=item=>empty?[]:[item];
function response(p){
 if(p==='/api/policies/status')return {allRequiredAccepted:true,missing:[]};
 if(p==='/api/auth/me')return {...fixtures.User,role};
 if(p==='/api/auth/refresh')return {error:'Session expired'};
 if(p==='/api/policies/current'||p==='/api/admin/policies')return empty?[]:['TERMS_OF_SERVICE','PRIVACY_POLICY','ACCEPTABLE_USE_POLICY','PRODUCTION_POLICY','INSURANCE_REQUIREMENTS'].map((policyType,i)=>({...fixtures.PolicyVersionDto,policyType,id:id.slice(0,-1)+(i+1)}));
 if(p==='/api/venues')return list(fixtures.VenueList);
 if(p==='/api/venues/my')return list(fixtures.Venue);
 if(p===`/api/venues/${id}`)return fixtures.Venue;
 if(p.endsWith('/availability/settings'))return fixtures.VenueAvailabilitySettings;
 if(p.endsWith('/moderation'))return fixtures.VenueModerationDto;
 if(p==='/api/owner/verification')return fixtures.OwnerVerificationDto;
 if(p==='/api/owner/payout-destinations')return list(fixtures.PayoutDestinationDto);
 if(p==='/api/owner/payouts')return page(fixtures.OwnerPayout);
 if(p===`/api/owner/payouts/${id}`)return fixtures.OwnerPayout;
 if(p==='/api/bookings')return list(fixtures.Booking);
 if(p===`/api/bookings/${id}`)return fixtures.Booking;
 if(p===`/api/bookings/${id}/incidents`)return list(fixtures.IncidentDto);
 if(p.startsWith('/api/availability-inquiries/'))return list(fixtures.AvailabilityInquiry);
 const admin={'bookings':'AdminBookingSummaryDto','incidents':'IncidentDto','liabilities':'OwnerFinancialLiabilityDto','owner-verifications':'AdminOwnerVerificationDto','payout-destinations':'PayoutDestinationDto','payouts':'OwnerPayoutDto','venues':'AdminVenueModerationDto','operations/exceptions':'OperationalExceptionDto'};
 for(const [segment,type] of Object.entries(admin)){
  if(p===`/api/admin/${segment}`)return page(fixtures[type]);
  if(p===`/api/admin/${segment}/${id}`)return fixtures[segment==='bookings'?'AdminBookingDetailDto':type];
 }
 if(p===`/api/admin/bookings/${id}/events`)return page(fixtures.BookingEventDto);
 if(p===`/api/admin/venues/${id}/events`)return list(fixtures.TrustEventDto);
 unexpected.push(p);return {};
}
function check(route,variant,pass,detail){const item={route,variant,result:pass?'PASS':'FAIL',...(detail?{detail}:{})};checks.push(item);if(!pass)failures.push(item);}
async function intercept(q){const p=new URL(q.request.url).pathname;requests.push({path:p,method:q.request.method});if(hold&&q.request.method!=='OPTIONS'&&p!='/api/policies/status'){held.push(q);return;}let status=q.request.method==='OPTIONS'?204:p==='/api/auth/refresh'?401:200,body=status===204?'':JSON.stringify(response(p));
 if(fault&&p===fault.path&&q.request.method!=='OPTIONS'){
  if(fault.kind==='offline'){await c.send('Fetch.failRequest',{requestId:q.requestId,errorReason:'InternetDisconnected'});return;}
  if(fault.kind==='invalid-json')body='{broken';else if(fault.kind==='wrong-shape')body='{}';else{status=fault.kind;body=JSON.stringify({message:status===400?'Please check this request.':'Synthetic server failure',trace:'INTERNAL_ONLY_MARKER'});}
 }
 await c.send('Fetch.fulfillRequest',{requestId:q.requestId,responseCode:status,responseHeaders:[{name:'Access-Control-Allow-Origin',value:'http://localhost:3026'},{name:'Access-Control-Allow-Headers',value:'authorization,content-type'},{name:'Access-Control-Allow-Methods',value:'GET,POST,OPTIONS'},{name:'Content-Type',value:'application/json'}],body:Buffer.from(body).toString('base64')});
}
async function account(r){role=r||'SCOUT';await c.evaluate(r?`localStorage.setItem('eema:auth',${JSON.stringify(JSON.stringify({accessToken:'synthetic-not-a-jwt',refreshToken:'synthetic-not-a-credential',expiresIn:3600,storedAt:Date.now(),generation:require('crypto').randomUUID(),user:{...fixtures.User,role:r}}))});sessionStorage.setItem('eema:bookingId','${id}')`:`localStorage.removeItem('eema:auth')`);}
async function settle(){await c.wait(`document.readyState==='complete' && !/Checking your account|Loading (operational data|your|venue|payout|owner|listing|current policies|booking details|venues)/.test(document.body.innerText)`,'page settled');await c.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))');}
async function inspect(route,width){const state=await c.evaluate(`(()=>{const visible=e=>!!(e.offsetWidth||e.offsetHeight||e.getClientRects().length);const missing=Array.from(document.querySelectorAll('input:not([type=hidden]),textarea,select')).filter(visible).filter(e=>!e.labels?.length&&!e.getAttribute('aria-label')&&!e.getAttribute('aria-labelledby')).map(e=>e.tagName+':'+e.type);return {crash:document.body.innerText.includes('Application error'),overflow:document.documentElement.scrollWidth>document.documentElement.clientWidth+1,width:innerWidth,scrollWidth:document.documentElement.scrollWidth,missingLabels:missing,headings:document.querySelectorAll('h1').length,main:document.querySelectorAll('main').length};})()`);
 check(route,`ready ${width}px`,!state.crash&&!state.overflow&&!state.missingLabels.length&&state.main===1,state);}
async function run(){const browser=await launch();c=browser.c;try{
 c.setIntercept(intercept);await c.send('Fetch.enable',{patterns:[{urlPattern:'*://*/api/*'}]});await c.go('/');
 const supplemental=process.argv.includes('--supplement')?JSON.parse(fs.readFileSync(path.join(out,'missing-loaders.json'),'utf8')):null;
 for(const item of inventory.filter(x=>!supplemental||supplemental[x.route]?.length)){const route=item.route.replace('[id]',id),r=route.startsWith('/admin')?'ADMIN':route.startsWith('/owner')?'OWNER':'SCOUT';fault=null;empty=false;await account(r);requests=[];
  await c.go(route);await settle();const loaders=supplemental?supplemental[item.route]:expectedLoaders[item.route];
  for(const width of supplemental?[]:[375,768,1440]){await c.send('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:false});await inspect(item.route,width);if(width===375)await c.screen(item.id.toLowerCase()+(process.argv.includes('--long')?'-long-mobile':'-mobile'));}
  if(process.argv.includes('--layout-only'))continue;
  for(const target of loaders){for(const kind of [400,401,403,404,409,500,'offline','invalid-json','wrong-shape']){await account(r);fault={path:target,kind};requests=[];await c.go(route);for(let n=0;n<150&&!requests.some(x=>x.path===target&&x.method==='GET');n++)await new Promise(r=>setTimeout(r,50));check(item.route,`${target} ${kind} reached`,requests.some(x=>x.path===target&&x.method==='GET'));await settle();const body=await c.evaluate('document.body.innerText');check(item.route,`${target} ${kind}`,!body.includes('Application error')&&!body.includes('INTERNAL_ONLY_MARKER')&&!!body.trim());}}
  if(supplemental)continue;
  fault=null;empty=true;await account(r);await c.go(route);await settle();check(item.route,'empty collections',!await c.evaluate(`document.body.innerText.includes('Application error')`));empty=false;
  if(route.startsWith('/admin')||route.startsWith('/owner')||route.startsWith('/scout')){for(const denied of [null,r==='SCOUT'?'OWNER':'SCOUT']){await account(denied);requests=[];await c.go(route);await settle();check(item.route,denied?'wrong role':'anonymous',!requests.some(x=>x.method==='GET'&&loaders.includes(x.path)));}}
  console.log(item.id,checks.length,failures.length);fs.writeFileSync(path.join(out,'browser-progress.json'),JSON.stringify({checks,failures,unexpected},null,2));
 }
 const result={result:failures.length||unexpected.length?'FAIL':'PASS',checks,failures,unexpected,provider:'Intercepted synthetic UI fixtures; no live provider or database mutation'};fs.writeFileSync(path.join(out,supplemental?'loader-supplement.json':process.argv.includes('--long')?'long-layout.json':process.argv.includes('--layout-only')?'layout.json':'browser.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify({checks:checks.length,failures:failures.length,unexpected}));
}finally{await browser.close();}}
module.exports={response,fixtures,id,account,settle,inspect,intercept};
if(require.main===module)run().catch(e=>{fs.writeFileSync(path.join(out,'browser-harness-error.json'),JSON.stringify({error:e.message,checks,failures,unexpected},null,2));console.error(e.message);process.exitCode=1;});
