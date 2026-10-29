#!/usr/bin/env node
// Existing TypeScript compiler only; contract checks do not calculate readiness.
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),frontend=path.resolve(root,'../Eema-frontend');
const ts=require(path.join(frontend,'node_modules/typescript'));
const source=fs.readFileSync(path.join(frontend,'lib/venue-readiness.ts'),'utf8');
const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
const moduleExports={};new Function('exports',compiled)(moduleExports);
const fixture=fs.readFileSync(path.join(root,'src/test/java/com/eema/backend/support/ReadyVenueFixture.java'),'utf8');
const keys=[...fixture.split('KEYS = List.of(')[1].split(');')[0].matchAll(/"([^"]+)"/g)].map(m=>m[1]);
assert.equal(keys.length,12);assert.deepEqual(Object.keys(moduleExports.READINESS_ISSUE_LABELS).sort(),[...keys].sort());
const labels=keys.map(key=>{const label=moduleExports.readinessIssueLabel(key);assert(label&&label!==key);return {key,label};});
assert.equal(moduleExports.readinessIssueLabel('futureIssue'),'futureIssue');
for(const file of ['components/venue/OwnerModerationPanel.tsx','app/admin/venues/[id]/page.tsx']){
 const text=fs.readFileSync(path.join(frontend,file),'utf8');assert(text.includes('@/lib/venue-readiness'));assert(text.includes('readinessIssueLabel(i)'));
}
fs.writeFileSync(path.join(root,'docs/qa/evidence/readiness/client.json'),JSON.stringify({result:'PASS',keys:12,labels,contract:'Both views explain the exact Spring key set; no local completeness calculation'},null,2)+'\n');
console.log('PASS: 12 readiness labels, both views share mapping');
