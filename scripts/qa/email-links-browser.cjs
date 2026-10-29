// Existing mail CTA routes in real Chromium. All API responses are synthetic.
const {launch}=require('./ux-cdp.cjs'),{response,fixtures:f,id}=require('./ux-browser.cjs');
const fs=require('fs'),assert=require('assert/strict');
(async()=>{
 const {c,close}=await launch();const checks=[],errors=[];let scenario={role:'SCOUT'},tokenForwarded=false;
 const token='synthetic+/token<&';
 const fulfill=(q,body,status=200)=>c.send('Fetch.fulfillRequest',{requestId:q.requestId,responseCode:status,responseHeaders:[
  {name:'Access-Control-Allow-Origin',value:'http://localhost:3026'},{name:'Access-Control-Allow-Headers',value:'authorization,content-type'},
  {name:'Access-Control-Allow-Methods',value:'GET,POST,OPTIONS'},{name:'Content-Type',value:'application/json'}],body:status===204?'':Buffer.from(JSON.stringify(body)).toString('base64')});
 try {
  c.setIntercept(async q=>{try{
   const p=new URL(q.request.url).pathname;
   if(q.request.method==='OPTIONS')return await fulfill(q,null,204);
   if(p==='/api/auth/login')return await fulfill(q,{accessToken:'synthetic-not-a-jwt',refreshToken:'synthetic-not-a-credential',expiresIn:3600,user:{...f.User,role:scenario.role}});
   if(p==='/api/auth/me')return await fulfill(q,{...f.User,role:scenario.role});
   if(p==='/api/auth/refresh')return await fulfill(q,{},401);
   if(p==='/api/auth/verify-email'||p==='/api/auth/reset-password'){
    tokenForwarded=JSON.parse(q.request.postData||'{}').token===token;return await fulfill(q,{message:"Synthetic success"},200);
   }
   return await fulfill(q,response(p));
  }catch(e){errors.push(e.message);await c.send('Fetch.failRequest',{requestId:q.requestId,errorReason:'Failed'}).catch(()=>{});}});
  await c.send('Fetch.enable',{patterns:[{urlPattern:'*://*/api/*'}]});
  const cases=[
   {role:'SCOUT',route:'/bookings/'+id,ready:'Requested'},
   {role:'OWNER',route:'/bookings/'+id,ready:'Requested'},
   {role:'OWNER',route:'/owner/inquiries',ready:f.AvailabilityInquiry.venueName},
   {role:'SCOUT',route:'/scout/inquiries',ready:f.AvailabilityInquiry.venueName},
   {role:'OWNER',route:'/owner/verification',ready:'Your owner review is complete.'},
   {role:'OWNER',route:'/owner/venues/'+id,ready:'Save timezone'},
   {role:'OWNER',route:'/owner/payout-destination',ready:'Payout destination verified'}];
  for(scenario of cases.filter(x=>!process.argv.includes("--venue-only")||x.route.startsWith("/owner/venues/"))){try{
   await c.go('/');await c.evaluate('localStorage.clear(); sessionStorage.clear()');await c.go(scenario.route);
   await c.wait("!!document.querySelector('form input[type=password]')",'inline sign-in');
   await c.fill(['synthetic@example.test','SyntheticLocalOnly!1']);await c.evaluate("document.querySelector('form').requestSubmit()");
   await c.wait("!document.querySelector('form input[type=password]') && document.body.textContent.includes("+JSON.stringify(scenario.ready)+")",'authenticated CTA content');
   assert.equal(await c.evaluate('location.pathname'),scenario.route);
   assert.equal(await c.evaluate("JSON.parse(localStorage.getItem('eema:auth')).user.role"),scenario.role);
   await c.go(scenario.route);await c.wait("!document.querySelector('form input[type=password]') && document.body.textContent.includes("+JSON.stringify(scenario.ready)+")",'reload content');
   assert.equal(await c.evaluate('document.body.innerText.includes("Application error")'),false);
   checks.push({route:scenario.route,role:scenario.role,signInPreservesContext:true,reloadShowsServerFixture:true,result:'PASS'});
  }catch(e){fs.writeFileSync('/tmp/eema-email-link-debug.json',JSON.stringify(await c.evaluate('({text:document.body.innerText,buttons:Array.from(document.querySelectorAll("button")).map(b=>b.textContent)})'),null,2));checks.push({route:scenario.route,role:scenario.role,result:'FAIL',error:e.message});}}
  for(const route of ['/verify-email','/reset-password']){try{
   await c.go('/');await c.evaluate('localStorage.clear(); sessionStorage.clear()');tokenForwarded=false;
   await c.go(route+'?token='+encodeURIComponent(token));
   if(route==='/reset-password'){
    await c.wait("!!document.querySelector('form input[type=password]')",'reset form');
    await c.fill(['SyntheticLocalOnly!1','SyntheticLocalOnly!1']);await c.evaluate("document.querySelector('form').requestSubmit()");
   }
   await c.wait('document.body.innerText.includes('+JSON.stringify(route==='/verify-email'?'Email verified':'Your password has been reset.')+')','token action success');
   assert.equal(tokenForwarded,true);checks.push({route,encodedTokenForwardedCorrectly:true,result:'PASS'});
  }catch(e){checks.push({route,result:'FAIL',error:e.message});}}
 }finally{await close();}
 const result={scope:'Actual production frontend and real browser; synthetic intercepted auth/domain responses, no real provider or backend transition',checks,errors,result:errors.length||checks.some(x=>x.result==='FAIL')?'FAIL':'PASS'};
 fs.writeFileSync(process.env.EEMA_EMAIL_OUTPUT||'docs/qa/evidence/email/browser-links.json',JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result));if(result.result!=='PASS')process.exitCode=1;
})().catch(e=>{console.error(e.message);process.exitCode=1});
