import { spawn, spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
const require=createRequire(import.meta.url);
const studio = process.platform==='win32' ? spawnSync('powershell.exe',
  ['-NoProfile','-Command','(Get-Process studio64 -ErrorAction SilentlyContinue | Select-Object -First 1).Path'],
  {encoding:'utf8'}).stdout?.trim() : '';
const candidates=[
  studio && join(dirname(studio),'..','jbr'),
  process.env.JAVA_HOME,process.env.STUDIO_JDK,
  process.env.ProgramFiles && join(process.env.ProgramFiles,'Android','Android Studio','jbr'),
  process.env.LOCALAPPDATA && join(process.env.LOCALAPPDATA,'Programs','Android Studio','jbr'),
].filter(Boolean);
let javaHome;
for(const candidate of candidates) {
  const java=join(candidate,'bin',process.platform==='win32'?'java.exe':'java');
  if(!existsSync(java)) continue;
  const r=spawnSync(java,['-version'],{encoding:'utf8'});
  const version=(r.stderr+r.stdout).match(/version "(\d+)/);
  if(version && Number(version[1])>=21){javaHome=candidate;break;}
}
const env={...process.env};
const pathKey=Object.keys(env).find(k=>k.toLowerCase()==='path') || 'PATH';
env[pathKey]=dirname(process.execPath)+(process.platform==='win32'?';':':')+(env[pathKey] || '');
if(javaHome){env.JAVA_HOME=javaHome;env[pathKey]=join(javaHome,'bin')+(process.platform==='win32'?';':':')+env[pathKey];}
const cli=require.resolve('firebase-tools/lib/bin/firebase.js');
const child=spawn(process.execPath,[cli,'emulators:exec','--config','../../firebase.json','--project','demo-discounimib','--only','firestore','"'+process.execPath+'" --test rules.test.mjs'],
  {cwd:fileURLToPath(new URL('.',import.meta.url)),env,stdio:'inherit'});
child.on('error',e=>{console.error(e.message);process.exitCode=1;});
child.on('exit',code=>{process.exitCode=code ?? 1;});
