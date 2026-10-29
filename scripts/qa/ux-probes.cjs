const {launch,out}=require('./ux-cdp.cjs'),fs=require('fs'),path=require('path');
const id='10000000-0000-0000-0000-000000000001';
const user={id,email:'ux@example.invalid',fullName:'QA UX Operator',role:'ADMIN',active:true,emailVerified:true,phone:null};
const session={accessToken:'synthetic-not-a-jwt',refreshToken:'synthetic-not-a-credential',expiresIn:3600,user};
(async()=>{const {c,close}=await launch(),results=[];let mode='wrong-shape';try{
 c.setIntercept(async q=>{const p=new URL(q.request.url).pathname;let body={};if(p.endsWith('/policies/status'))body={allRequiredAccepted:true,missing:[]};if(p.includes('/payout-destinations/'))body={id,ownerId:id,type:'IBAN',currency:'EUR',accountHolderName:'QA UX Holder',maskedIban:'NL•• •••• •••• 1234',status:'VERIFIED',submittedAt:'2026-09-20T10:00:00Z',verifiedAt:null,rejectedAt:null,rejectionReason:null,replacedAt:null,anotherVerifiedDestinationExists:false};
 await c.send('Fetch.fulfillRequest',{requestId:q.requestId,responseCode:200,responseHeaders:[{name:'Access-Control-Allow-Origin',value:'http://localhost:3026'},{name:'Access-Control-Allow-Headers',value:'authorization,content-type'},{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(body)).toString('base64')});});await c.send('Fetch.enable',{patterns:[{urlPattern:'*://*/api/*'}]});
 await c.go('/');await c.evaluate(`localStorage.setItem('eema:auth',${JSON.stringify(JSON.stringify(session))})`);
 for(const route of ['/admin/bookings','/admin/policies','/scout/inquiries']){
  if(route.startsWith('/scout')){session.user.role='SCOUT';await c.evaluate(`localStorage.setItem('eema:auth',${JSON.stringify(JSON.stringify(session))})`);}
  await c.go(route);await c.wait(`document.body.textContent.includes('Application error') || document.body.textContent.includes('Try again')`,'malformed result');
  results.push({route,variant:'200 {}',crashed:await c.evaluate(`document.body.textContent.includes('Application error')`)});await c.screen('initial-'+route.split('/').filter(Boolean).join('-'));
 }
 session.user.role='ADMIN';await c.evaluate(`localStorage.setItem('eema:auth',${JSON.stringify(JSON.stringify(session))})`);await c.go('/admin/payout-destinations/'+id);await c.wait(`document.body.textContent.includes('Reveal full IBAN')`,'destination');
 await c.evaluate(`Array.from(document.querySelectorAll('button')).find(b=>b.textContent==='Reveal full IBAN').focus()`);await c.send('Input.dispatchKeyEvent',{type:'keyDown',key:'Enter',code:'Enter',windowsVirtualKeyCode:13});await c.send('Input.dispatchKeyEvent',{type:'keyUp',key:'Enter',code:'Enter',windowsVirtualKeyCode:13});
 await c.click('Reveal full IBAN');await c.wait(`!!document.querySelector('[role=alertdialog]')`,'reveal confirmation');results.push({route:'/admin/payout-destinations/[id]',variant:'keyboard modal open',focusInside:await c.evaluate(`document.querySelector('[role=alertdialog]').contains(document.activeElement)`)});await c.screen('initial-reveal-focus');
 fs.writeFileSync(path.join(out,'history/initial-probes.json'),JSON.stringify({results},null,2)+'\n');console.log(JSON.stringify(results));
}finally{await close();}})().catch(e=>{console.error(e.message);process.exitCode=1;});
