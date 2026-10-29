const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),front=path.resolve(root,'../Eema-frontend'),ts=require(path.join(front,'node_modules/typescript'));
const source=fs.readFileSync(path.join(front,'lib/pricing.ts'),'utf8');const m={};new Function('exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(m);
const cases=JSON.parse(fs.readFileSync(path.join(root,'src/test/resources/pricing-cases.json'))),failed=[];
const time=n=>String(Math.floor(n/60)).padStart(2,'0')+':'+String(n%60).padStart(2,'0');
for(const x of cases){const q=m.calculatePricingPreview('00:00',time(x.duration),Number(x.hourly),x.minimum,x.cap===null?null:Number(x.cap));const displayed=new Intl.NumberFormat('en-GB',{minimumFractionDigits:2,maximumFractionDigits:2,useGrouping:false}).format(q.estimatedPrice);if(q.estimatedPrice!==Number(x.expected)||displayed!==x.expected)failed.push({id:x.id,expected:x.expected,actual:q.estimatedPrice,displayed});if(!process.argv.includes('--probe')){assert.equal(q.estimatedPrice,Number(x.expected),x.id);assert.equal(q.hourlyTotal,Number(x.hourlyTotal));assert.equal(q.billableMinutes,x.billable);assert.equal(displayed,x.expected);}}
if(!process.argv.includes('--probe')) {
  assert.equal(m.calculatePricingPreview('12:00','13:00',99999999.99,60,null).estimatedPrice,99999999.99);
  assert.equal(m.calculatePricingPreview('12:00','13:00',99999999.99,10080,null),null);
  assert.equal(m.calculatePricingPreview('12:00','13:00',99999999.99,10080,500).estimatedPrice,500);
  for(const args of [['12:00','12:00',100,60,null],['23:00','02:00',100,60,null],['12:00','13:00',0,60,null],['12:00','13:00',Infinity,60,null],['12:00','13:00',100,0,null],['12:00','13:00',100,60,-1]])assert.equal(m.calculatePricingPreview(...args),null);
}
fs.writeFileSync(path.join(root,'docs/qa/evidence/price',process.argv.includes('--probe')?'client-before.json':'client.json'),JSON.stringify({result:failed.length?'REPRODUCED':'PASS',cases:cases.length,failures:failed},null,2));console.log({cases:cases.length,failures:failed.length});
