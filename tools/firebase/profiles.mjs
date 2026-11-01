import { readFileSync } from 'node:fs';
import { getAuth } from 'firebase-admin/auth';
import { Timestamp } from '@google-cloud/firestore';
import { deleteApp } from 'firebase-admin/app';
import { adminApp, projectId } from './admin.mjs';
// Non stampare oggetti di errore SDK: possono includere dettagli della richiesta OAuth.
const fatal=e=>{
  const code=typeof e?.code==='string' || typeof e?.code==='number' ? String(e.code) : 'ADMIN_ERROR';
  console.error('Operazione profili non riuscita ('+code+'). '+(e?.config || e?.response || e?.errorInfo ? 'Verifica accesso CLI e permessi.' : String(e?.message || 'Errore').slice(0,400)));
  process.exit(1);
};
process.on('uncaughtException',fatal);
process.on('unhandledRejection',fatal);
const mode=process.argv[2] || 'list';
if (!['list','plan','apply'].includes(mode)) throw new Error('Usa list, plan o apply.');
const {app,db}=await adminApp(),auth=getAuth(app);
const publicProfile=p=>Object.fromEntries(['uid','nome','cognome','ruolo','giornoRicevimento','oraInizio','oraFine','durataAppuntamento','disponibilitaVersione'].map(k=>[k,p[k]]));
const text=(s,min,max)=>typeof s==='string' && s.trim().length>=min && s.length<=max;
const validSchedule=o=>{
  const days=['Lunedì','Martedì','Mercoledì','Giovedì','Venerdì','Sabato','Domenica'];
  const time=s=>typeof s==='string' && /^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(s);
  const mins=s=>Number(s.slice(0,2))*60+Number(s.slice(3));
  return days.includes(o.giornoRicevimento) && time(o.oraInizio) && time(o.oraFine)
    && Number.isInteger(o.durataAppuntamento) && o.durataAppuntamento>0
    && o.durataAppuntamento<=mins(o.oraFine)-mins(o.oraInizio);
};
try {
  if (mode==='list') {
    let page;
    do {
      const result=await auth.listUsers(1000,page);
      for(const u of result.users) {
        if(!/^[A-Za-z0-9_-]{1,128}$/.test(u.uid)) {console.log(JSON.stringify({uid:u.uid,email:u.email,errore:'UID non compatibile con i percorsi app'}));continue;}
        const d=await db.doc('users/'+u.uid).get();
        console.log(JSON.stringify({uid:u.uid,email:u.email || null,nomeAccount:u.displayName || null,nome:d.get('nome') ?? null,cognome:d.get('cognome') ?? null,matricola:d.get('matricola') ?? null,ruolo:d.exists?d.get('ruolo'):null,profiloEsistente:d.exists,emailVerificata:u.emailVerified,disabilitato:u.disabled}));
      }
      page=result.pageToken;
      if (!result.users.length && !page) console.log('Nessun account Authentication trovato.');
    } while(page);
  } else {
    const manifest=JSON.parse(readFileSync(new URL('./profiles.local.json',import.meta.url),'utf8'));
    if(manifest.projectId!==projectId || !Array.isArray(manifest.profiles) || !manifest.profiles.length) throw new Error('Manifest del progetto mancante o vuoto.');
    const emails=new Set();
    for(const p of manifest.profiles) {
      if(Object.keys(p).some(k=>!['uid','email','nome','cognome','ruolo','matricola','orario'].includes(k))
        || !text(p.email,3,320) || !p.email.includes('@') || !text(p.nome,1,100) || !text(p.cognome,1,100)
        || !['studente','professore'].includes(p.ruolo) || !text(p.matricola ?? '',0,64)) throw new Error('Profilo non valido: controlla campi, nome, email e ruolo.');
      const email=p.email.toLowerCase();if(emails.has(email))throw new Error('Email duplicata.');emails.add(email);
      if(p.orario) {
        const o=p.orario,days=['Lunedì','Martedì','Mercoledì','Giovedì','Venerdì','Sabato','Domenica'];
        const validTime=s=>typeof s==='string' && /^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(s);
        const mins=s=>Number(s.slice(0,2))*60+Number(s.slice(3));
        if(p.ruolo!=='professore' || Object.keys(o).some(k=>!['giornoRicevimento','oraInizio','oraFine','durataAppuntamento'].includes(k))
          || !days.includes(o.giornoRicevimento) || !validTime(o.oraInizio) || !validTime(o.oraFine)
          || !Number.isInteger(o.durataAppuntamento) || o.durataAppuntamento<=0 || o.durataAppuntamento>mins(o.oraFine)-mins(o.oraInizio))throw new Error('Orario non valido.');
      }
    }
    for(const p of manifest.profiles) {
      let u;try{u=await auth.getUserByEmail(p.email);}catch(e){if(e.code!=='auth/user-not-found')throw e;}
      if(p.uid && p.uid!==u?.uid)throw new Error('UID diverso da quello Authentication per '+p.email);
      if(u?.disabled)throw new Error('Account disabilitato: '+p.email);
      if(p.ruolo==='professore' && u && !u.emailVerified && !p.uid) {
        if(mode==='plan'){console.log(JSON.stringify({email:p.email,uid:u.uid,blocco:'Conferma questo UID nel manifest oppure verifica prima la email del docente.'}));continue;}
        throw new Error('Per il docente con email non verificata indica esplicitamente il suo UID Authentication nel manifest.');
      }
      if(mode==='plan'){console.log(JSON.stringify({email:p.email,uid:u?.uid || null,azione:u?'crea/aggiorna profilo':'crea account senza password e profilo',ruolo:p.ruolo}));continue;}
      if(!u)u=await auth.createUser({email:p.email,displayName:p.nome.trim()+' '+p.cognome.trim()});
      await db.runTransaction(async tx=>{
        if(!/^[A-Za-z0-9_-]{1,128}$/.test(u.uid))throw new Error('UID Authentication non compatibile con i percorsi dell’app.');
        const ref=db.doc('users/'+u.uid),old=await tx.get(ref),data=old.data() || {};
        const keys=['uid','email','nome','cognome','ruolo','matricola','dataCreazione','ultimoAccesso',
          'giornoRicevimento','oraInizio','oraFine','durataAppuntamento','disponibilitaVersione'];
        if(Object.keys(data).some(k=>!keys.includes(k)))throw new Error('Il profilo contiene campi aggiuntivi: verificarli prima della migrazione.');
        if(!text(p.matricola ?? data.matricola ?? '',0,64))throw new Error('Matricola non valida nel profilo esistente.');
        if(data.ruolo==='professore' && p.ruolo!=='professore')throw new Error('La revoca del ruolo docente richiede una procedura amministrativa dedicata.');
        const profile={uid:u.uid,email:u.email,nome:p.nome.trim(),cognome:p.cognome.trim(),ruolo:p.ruolo,
          matricola:p.matricola ?? data.matricola ?? '',dataCreazione:data.dataCreazione instanceof Timestamp ? data.dataCreazione : Timestamp.now(),ultimoAccesso:data.ultimoAccesso instanceof Timestamp ? data.ultimoAccesso : Timestamp.now()};
        if(p.ruolo==='professore'){
          const defaults={giornoRicevimento:'Lunedì',oraInizio:'09:00',oraFine:'12:00',durataAppuntamento:30};
          const schedule=p.orario || Object.fromEntries(Object.entries(defaults).map(([k,v])=>[k,data[k] ?? v]));
          if(!validSchedule(schedule))throw new Error('Orario docente non valido: indica un orario corretto nel manifest.');
          const version=data.disponibilitaVersione ?? 0;
          if(!Number.isInteger(version) || version<0 || version>=999999999)throw new Error('Versione disponibilità non valida.');
          const changed=old.exists && data.ruolo==='professore' && Object.entries(schedule).some(([k,v])=>data[k]!==v);
          Object.assign(profile,schedule,{disponibilitaVersione:version+(changed?1:0)});
          tx.set(db.doc('professori/'+u.uid),publicProfile(profile));
        }
        tx.set(ref,profile);
      });
      console.log(JSON.stringify({uid:u.uid,email:u.email,ruolo:p.ruolo,profilo:'salvato'}));
    }
  }
} finally {await db.terminate();await deleteApp(app);}
