#!/usr/bin/env node
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),frontend=path.resolve(root,'../Eema-frontend'),ts=require(path.join(frontend,'node_modules/typescript'));
class ApiError extends Error {constructor(status,message){super(message);this.status=status;}}
const source=fs.readFileSync(path.join(frontend,'lib/api/error-message.ts'),'utf8');
const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
const exported={};new Function('exports','require',compiled)(exported,()=>({ApiError}));
const message=exported.apiErrorMessage(new ApiError(413,'Image exceeds the configured maximum size'));
if(process.argv.includes('--probe')){
 fs.writeFileSync(path.join(root,'docs/qa/evidence/media/size-error-before.json'),JSON.stringify({status:413,message,actionable:/too large|size limit|maximum size/i.test(message)},null,2));
}else{
 assert.match(message,/too large|size limit|maximum size/i);
 assert.match(exported.apiErrorMessage(new ApiError(400,'Invalid or corrupt image; upload a valid JPEG, PNG, or WebP photo')),/Invalid or corrupt/);
 assert.equal(exported.apiErrorMessage(new ApiError(500,'S3 secret provider internals')),'This operation is temporarily unavailable. Please try again.');
 fs.writeFileSync(path.join(root,'docs/qa/evidence/media/client.json'),JSON.stringify({result:'PASS',checks:3,oversizeMessage:message,corruptMessage:'Actionable validation message',providerFailure:'Generic safe client message'},null,2));
}
