#!/usr/bin/env node
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),frontend=path.resolve(root,'../Eema-frontend'),ts=require(path.join(frontend,'node_modules/typescript'));
if(process.argv.includes('--probe')){
 const source=fs.readFileSync(path.join(frontend,'components/checkout/InstantQuote.tsx'),'utf8');
 const ast=ts.createSourceFile('quote.tsx',source,ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX);
 const fn=ast.statements.find(s=>ts.isFunctionDeclaration(s)&&s.name.text==='todayIso');
 const compiled=ts.transpileModule(fn.getText(ast),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
 process.env.TZ='Pacific/Kiritimati';class FixedDate extends Date {constructor(...args){super(...(args.length?args:['2026-10-24T21:30:00Z']));}}
 const actual=new Function('Date',compiled+';return todayIso();')(FixedDate);
 assert.equal(actual,'2026-10-25');
 fs.writeFileSync(path.join(root,'docs/qa/evidence/availability/client-before.json'),JSON.stringify({result:'REPRODUCED',browserTimezone:'Pacific/Kiritimati',venueTimezone:'Europe/Amsterdam',instant:'2026-10-24T21:30:00Z',actualToday:actual,expectedToday:'2026-10-24',impact:'Valid venue-local date disabled when browser is a day ahead'},null,2));
 console.log('Reproduced browser-local day defect');
}
else {
 const source=fs.readFileSync(path.join(frontend,'lib/availability-calendar.ts'),'utf8');const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
 const m={};new Function('exports',compiled)(m);let checks=0;
 for(const browserZone of ['America/Los_Angeles','Pacific/Kiritimati','UTC']){
  process.env.TZ=browserZone;
  assert.equal(m.todayInZone('Europe/Amsterdam',new Date('2026-10-24T21:30:00Z')),'2026-10-24');checks++;
  assert.equal(m.todayInZone('Europe/Amsterdam',new Date('2026-12-31T23:30:00Z')),'2027-01-01');checks++;
 }
 for(let month=0;month<12;month++){
  const value={year:2028,month};const count=new Date(Date.UTC(2028,month+1,0)).getUTCDate();
  assert.equal(m.monthStart(value),m.isoDate(2028,month,1));assert.equal(m.monthEnd(value),m.isoDate(2028,month,count));checks++;
 }
 assert.deepEqual(m.offsetMonth({year:2026,month:11},1),{year:2027,month:0});assert.deepEqual(m.offsetMonth({year:2027,month:0},-1),{year:2026,month:11});checks+=2;
 fs.writeFileSync(path.join(root,'docs/qa/evidence/availability/client.json'),JSON.stringify({result:'PASS',checks,contract:'Venue-zone dates independent of browser timezone; calendar month arithmetic only, no client inventory calculation'},null,2));console.log('PASS calendar date checks:',checks);
}
