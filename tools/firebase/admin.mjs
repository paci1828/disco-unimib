import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { initializeApp } from 'firebase-admin/app';
import { Firestore } from '@google-cloud/firestore';
const require = createRequire(import.meta.url);
const cliAuth = require('firebase-tools/lib/auth');
const { requireAuth } = require('firebase-tools/lib/requireAuth');
const api = require('firebase-tools/lib/api');
export const projectId = 'discounimib-2026-db0ad';
// Adapter locale per la sessione ottenuta con la CLI ufficiale.
// Nessuna chiave di servizio, password o credenziale viene scritta nel progetto.
export async function adminApp() {
  if (process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) {
    throw new Error('I profili reali richiedono un ambiente senza variabili emulator.');
  }
  const root = fileURLToPath(new URL('../../',import.meta.url));
  const account = cliAuth.getProjectDefaultAccount(root);
  if (!account) throw new Error('Accedi prima con npx -y firebase-tools@latest login.');
  const options = {project:projectId,projectId,projectRoot:root,nonInteractive:true};
  cliAuth.setActiveAccount(options,account);
  await requireAuth(options);
  const credential = {getAccessToken:async()=>{
    const t = await cliAuth.getAccessToken(account.tokens.refresh_token,api.getScopes());
    if (!t.access_token) throw new Error('Sessione Firebase non disponibile.');
    return {access_token:t.access_token,expires_in:t.expires_in || Math.max(1,Math.floor(((t.expires_at || Date.now()+3600000)-Date.now())/1000))};
  }};
  const app = initializeApp({projectId,credential});
  const db = new Firestore({projectId,credentials:{
    type:'authorized_user',client_id:api.clientId(),client_secret:api.clientSecret(),
    refresh_token:account.tokens.refresh_token,quota_project_id:projectId
  }});
  return {app,db};
}
