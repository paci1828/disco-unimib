import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, deleteDoc, writeBatch, Timestamp, deleteField, setLogLevel } from 'firebase/firestore';

setLogLevel('silent');
let env;
const projectId = 'demo-discounimib';
const profId = 'prof';
const me = id => env.authenticatedContext(id, {email: id+'@example.org'}).firestore();
const now = () => Timestamp.now();
const monday = new Date();
monday.setUTCDate(monday.getUTCDate() + ((8 - monday.getUTCDay()) % 7 || 7) + 7);
const date = [monday.getUTCDate(),monday.getUTCMonth()+1,monday.getUTCFullYear()].map((n,i)=>i<2?String(n).padStart(2,'0'):n).join('/');
const person = (uid,ruolo='studente') => ({uid,email:uid+'@example.org',nome:uid,cognome:'Test',ruolo,matricola:'',dataCreazione:now(),ultimoAccesso:now(),
  ...(ruolo==='professore'?{giornoRicevimento:'Lunedì',oraInizio:'09:00',oraFine:'12:00',durataAppuntamento:30,disponibilitaVersione:0}:{})});
const publicProf = p => Object.fromEntries(['uid','nome','cognome','ruolo','giornoRicevimento','oraInizio','oraFine','durataAppuntamento','disponibilitaVersione'].map(k=>[k,p[k]]));
const app = (id='a1',extra={}) => stamp({id,idProfessore:profId,idStudente:'s1',data:date,oraInizio:'09:00',oraFine:'09:30',motivazione:'Tesi',stato:'sospeso',dataCreazione:now(),
  nomeStudente:'s1 Test',emailStudente:'s1@example.org',nomeProfessore:'prof Test',emailProfessore:null,
  revisione:0,sincronizzataRevisione:0,calendarioRevisione:0,emailRevisione:0,statoSincronizzazione:null,statoEmail:null,
  erroreSincronizzazione:null,eventoGoogleCalendarId:null,appuntamentoPrecedenteId:null,sostituitoDaId:null,...extra});
function instant(data,ora) {
  const [d,m,y]=data.split('/').map(Number),[h,min]=ora.split(':').map(Number);
  const local=Date.UTC(y,m-1,d,h,min);
  const offset=new Intl.DateTimeFormat('en',{timeZone:'Europe/Rome',timeZoneName:'shortOffset'})
    .formatToParts(new Date(local)).find(p=>p.type==='timeZoneName').value;
  const hours=Number(offset.replace('GMT',''));
  return Timestamp.fromMillis(local-hours*3600000);
}
function stamp(a) {return {...a,inizioIstante:instant(a.data,a.oraInizio),fineIstante:instant(a.data,a.oraFine)};}
const occupation = a => Object.fromEntries(['idProfessore','data','oraInizio','oraFine','stato'].map(k=>[k,a[k]]));
const slot = a => a.idProfessore+'_'+a.inizioIstante.toMillis();
function appendNew(b,db,a) {
  b.set(doc(db,'appuntamenti',a.id),a);
  b.set(doc(db,'occupazioni',a.id),occupation(a));
  b.set(doc(db,'slotPrenotati',slot(a)),{appuntamentoId:a.id,idProfessore:a.idProfessore,data:a.data,oraInizio:a.oraInizio});
}
const create = (db,a) => {const b=writeBatch(db);appendNew(b,db,a);return b.commit();};
async function transition(db,a,status,extra={}) {
  const b=writeBatch(db);
  const next={...a,stato:status,revisione:a.revisione+1,statoSincronizzazione:'in_attesa',erroreSincronizzazione:null,...extra};
  b.update(doc(db,'appuntamenti',a.id),{stato:status,revisione:next.revisione,statoSincronizzazione:'in_attesa',erroreSincronizzazione:null,...extra});
  b.set(doc(db,'occupazioni',a.id),occupation(next));
  if (['rifiutato','annullato'].includes(status)) b.delete(doc(db,'slotPrenotati',slot(a)));
  await b.commit();return next;
}
async function seedApp(a) {
  await env.withSecurityRulesDisabled(async c=>{await create(c.firestore(),a);});
}
before(async()=>{
  const rules=readFileSync(new URL('../../firestore.rules',import.meta.url),'utf8');
  console.log('Regole caricate: Timestamp='+rules.includes('inizioIstante')+', schema iniziale='+rules.includes('initialShape')+', caratteri='+rules.length);
  env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8089,rules}});
});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{await env.clearFirestore();await env.withSecurityRulesDisabled(async c=>{
  const db=c.firestore();for(const p of [person('prof','professore'),person('otherProf','professore'),person('s1'),person('s2')]) {
    await setDoc(doc(db,'users',p.uid),p);if(p.ruolo==='professore') await setDoc(doc(db,'professori',p.uid),publicProf(p));
  }
});});
test('anonimo non legge nessuna collezione',async()=>{
  const db=env.unauthenticatedContext().firestore();
  for(const name of ['users','professori','appuntamenti','occupazioni','slotPrenotati','indisponibilita','assenze']) await assertFails(getDocs(collection(db,name)));
});
test('profilo privato leggibile solo dal proprietario, lista generale negata',async()=>{
  await assertSucceeds(getDoc(doc(me('s1'),'users','s1')));
  await assertFails(getDoc(doc(me('s2'),'users','s1')));
  await assertFails(getDoc(doc(me('s1'),'users','prof')));
  await assertFails(getDocs(collection(me('s1'),'users')));
});
test('registrazione studente legata a uid ed email autenticati',async()=>{
  await assertSucceeds(setDoc(doc(me('new'),'users','new'),person('new')));
  await assertFails(setDoc(doc(me('new2'),'users','other'),person('other')));
  await assertFails(setDoc(doc(me('new2'),'users','new2'),{...person('new2'),email:'fake@example.org'}));
});
test('nessuna autoassegnazione del ruolo docente alla creazione o modifica',async()=>{
  await assertFails(setDoc(doc(me('new'),'users','new'),person('new','professore')));
  await assertFails(updateDoc(doc(me('s1'),'users','s1'),{ruolo:'professore'}));
});
test('validazione anche su aggiornamenti, campi obbligatori ed extra',async()=>{
  const ref=doc(me('s1'),'users','s1');
  await assertSucceeds(updateDoc(ref,{nome:'Nome nuovo'}));
  for(const edit of [{nome:'x'.repeat(101)},{nome:13},{nome:deleteField()},{isAdmin:true},{uid:'s2'},{email:'other@example.org'},{dataCreazione:Timestamp.fromMillis(0)}])
    await assertFails(updateDoc(ref,edit));
  await assertFails(deleteDoc(ref));
});
test('orario salvato con versione e profilo pubblico in una sola operazione',async()=>{
  const db=me('prof'),p=person('prof','professore');p.oraInizio='10:00';p.disponibilitaVersione=1;
  await assertFails(updateDoc(doc(db,'users','prof'),{oraInizio:p.oraInizio,disponibilitaVersione:1}));
  const b=writeBatch(db);b.update(doc(db,'users','prof'),{oraInizio:p.oraInizio,disponibilitaVersione:1});b.set(doc(db,'professori','prof'),publicProf(p));
  await assertSucceeds(b.commit());
  await assertSucceeds(updateDoc(doc(db,'users','prof'),{ultimoAccesso:now()}));
});
test('profilo pubblico non può contenere email né essere falsificato',async()=>{
  const p=publicProf(person('prof','professore'));
  await assertSucceeds(getDocs(query(collection(me('s1'),'professori'),where('ruolo','==','professore'))));
  await assertFails(setDoc(doc(me('s1'),'professori','prof'),p));
  await assertFails(updateDoc(doc(me('prof'),'professori','prof'),{email:'secret@example.org'}));
  await assertFails(updateDoc(doc(me('prof'),'professori','prof'),{durataAppuntamento:0}));
});
test('prenotazione con copie e blocco atomici',async()=>{
  const a=app();await assertSucceeds(create(me('s1'),a));
  await assertSucceeds(getDoc(doc(me('s1'),'appuntamenti',a.id)));
  await assertSucceeds(getDoc(doc(me('prof'),'appuntamenti',a.id)));
  await assertFails(getDoc(doc(me('s2'),'appuntamenti',a.id)));
  const o=(await getDoc(doc(me('s2'),'occupazioni',a.id))).data();assert.deepEqual(o,occupation(a));assert.equal(o.emailStudente,undefined);
});
test('query reali dei partecipanti e disponibilità senza lettura appuntamenti altrui',async()=>{
  await seedApp(app());
  await assertSucceeds(getDocs(query(collection(me('s1'),'appuntamenti'),where('idStudente','==','s1'))));
  await assertSucceeds(getDocs(query(collection(me('prof'),'appuntamenti'),where('idProfessore','==','prof'),where('stato','==','sospeso'))));
  await assertSucceeds(getDocs(query(collection(me('s2'),'occupazioni'),where('idProfessore','==','prof'))));
  await assertFails(getDocs(query(collection(me('s2'),'appuntamenti'),where('idProfessore','==','prof'))));
});
test('prenotazione senza copie o blocco e proiezioni falsificate negate',async()=>{
  await assertFails(setDoc(doc(me('s1'),'appuntamenti','a1'),app()));
  await assertFails(setDoc(doc(me('s1'),'occupazioni','fake'),occupation(app())));
  const db=me('s1'),b=writeBatch(db);appendNew(b,db,app());b.set(doc(db,'occupazioni','a1'),{...occupation(app()),motivazione:'privata'});
  await assertFails(b.commit());
});
test('nessuna impersonificazione di studente o conferma diretta',async()=>{
  for(const edit of [{idStudente:'s2',nomeStudente:'s2 Test',emailStudente:'s2@example.org'},{emailStudente:'fake@example.org'},{nomeStudente:'Nome falso'},{stato:'accettato'},{revisione:10},{eventoGoogleCalendarId:'fake'},{emailProfessore:'leak@example.org'}])
    await assertFails(create(me('s1'),app('a1',edit)));
});
test('controllo formato, data reale, passato, durata, griglia e orario',async()=>{
  for(const edit of [{data:'31/02/2028'},{data:'01/01/2020'},{oraInizio:'09:05',oraFine:'09:35'},{oraFine:'09:45'},{oraInizio:'25:00'},{oraInizio:'12:00',oraFine:'12:30'},{motivazione:'x'.repeat(4001)}])
    await assertFails(create(me('s1'),app('a1',edit)));
});
test('testi data e ora non possono contraddire gli istanti effettivi',async()=>{
  const a=app();
  const weekLater=new Date(monday.getTime()+7*86400000);
  a.data=String(weekLater.getUTCDate()).padStart(2,'0')+'/'+String(weekLater.getUTCMonth()+1).padStart(2,'0')+'/'+weekLater.getUTCFullYear();
  await assertFails(create(me('s1'),a));
  const b=app('a2');b.oraInizio='09:30';b.oraFine='10:00';
  await assertFails(create(me('s1'),b));
});
test('giorni di cambio ora: gli slot validi passano, ore ambigue o inesistenti negate',async()=>{
  const year=new Date().getUTCFullYear()+1;
  await env.withSecurityRulesDisabled(async c=>{
    const p=person('prof','professore');p.giornoRicevimento='Domenica';p.oraInizio='01:00';p.oraFine='04:00';
    await setDoc(doc(c.firestore(),'users','prof'),p);await setDoc(doc(c.firestore(),'professori','prof'),publicProf(p));
  });
  for(const month of [3,10]) {
    const d=new Date(Date.UTC(year,month,0));d.setUTCDate(d.getUTCDate()-d.getUTCDay());
    const value=String(d.getUTCDate()).padStart(2,'0')+'/'+String(month).padStart(2,'0')+'/'+year;
    await assertSucceeds(create(me('s1'),app('dst'+month,{data:value,oraInizio:'03:00',oraFine:'03:30'})));
    await assertFails(create(me('s1'),app('bad'+month,{data:value,oraInizio:'02:00',oraFine:'02:30'})));
  }
});
test('secondo documento sul medesimo slot non può sovrascrivere il blocco',async()=>{
  await assertSucceeds(create(me('s1'),app()));
  await assertFails(create(me('s2'),app('a2',{idStudente:'s2',nomeStudente:'s2 Test',emailStudente:'s2@example.org'})));
});
test('solo il docente accetta, poi lo studente può annullare',async()=>{
  const a=app();await seedApp(a);
  await assertFails(transition(me('s1'),a,'accettato'));
  const accepted=await assertSucceeds(transition(me('prof'),a,'accettato'));
  await assertSucceeds(transition(me('s1'),accepted,'annullato'));
  assert.equal((await getDoc(doc(me('s1'),'slotPrenotati',slot(a)))).exists(),false);
});
test('rifiuto libera lo slot preservando lo storico',async()=>{
  const a=app();await seedApp(a);const refused=await assertSucceeds(transition(me('prof'),a,'rifiutato'));
  await assertFails(transition(me('prof'),refused,'accettato'));
  await assertSucceeds(create(me('s2'),app('a2',{idStudente:'s2',nomeStudente:'s2 Test',emailStudente:'s2@example.org'})));
  assert.equal((await getDoc(doc(me('s1'),'appuntamenti',a.id))).data().stato,'rifiutato');
});
test('identità e contenuto immutabili, anche per il docente',async()=>{
  await seedApp(app());
  for(const actor of ['s1','prof']) for(const edit of [{idStudente:'s2'},{motivazione:'alterata'},{oraInizio:'10:00'},{dataCreazione:now()},{data:deleteField()},{id:deleteField()},{nomeProfessore:deleteField()},{inizioIstante:Timestamp.now()},{fineIstante:deleteField()},{admin:true}])
    await assertFails(updateDoc(doc(me(actor),'appuntamenti','a1'),edit));
});
test('solo docente partecipante aggiorna la sincronizzazione, con tipi e limiti',async()=>{
  const a=app('a1',{stato:'accettato',revisione:1});await seedApp(a);
  const edit={syncLeaseToken:'lease',syncLeaseFino:Date.now()+600000,statoSincronizzazione:'in_corso'};
  await assertSucceeds(updateDoc(doc(me('prof'),'appuntamenti','a1'),edit));
  await assertFails(updateDoc(doc(me('s1'),'appuntamenti','a1'),edit));
  await assertFails(updateDoc(doc(me('otherProf'),'appuntamenti','a1'),edit));
  for(const x of [{erroreSincronizzazione:'x'.repeat(8193)},{emailRevisione:2},{revisione:50},{syncLeaseFino:Date.now()+3600000}])
    await assertFails(updateDoc(doc(me('prof'),'appuntamenti','a1'),x));
  await assertSucceeds(updateDoc(doc(me('prof'),'appuntamenti','a1'),{calendarioRevisione:1,emailRevisione:1,statoEmail:'in_invio'}));
});
async function reschedule(actor,removeOld=true) {
  const db=me(actor),old=app(),a=app('a2',{oraInizio:'10:00',oraFine:'10:30',appuntamentoPrecedenteId:'a1',revisione:1,
    stato:actor==='prof'?'accettato':'sospeso',sincronizzataRevisione:actor==='prof'?0:1,statoSincronizzazione:actor==='prof'?'in_attesa':'non_richiesta'});
  const b=writeBatch(db);appendNew(b,db,a);
  if(removeOld){b.update(doc(db,'appuntamenti','a1'),{stato:'annullato',revisione:1,statoSincronizzazione:'in_attesa',erroreSincronizzazione:null,sostituitoDaId:'a2'});
    b.set(doc(db,'occupazioni','a1'),occupation({...old,stato:'annullato'}));b.delete(doc(db,'slotPrenotati',slot(old)));}
  return b.commit();
}
test('riprogrammazione studente atomica resta sospesa',async()=>{await seedApp(app());await assertSucceeds(reschedule('s1'));});
test('riprogrammazione docente atomica è accettata',async()=>{await seedApp(app());await assertSucceeds(reschedule('prof'));});
test('nessun collegamento senza annullamento atomico o da estranei',async()=>{
  await seedApp(app());await assertFails(reschedule('s1',false));await assertFails(reschedule('s2'));
});
test('cancellazioni nel passato e cancellazione storico negate',async()=>{
  const a=app('a1',{data:'01/01/2020'});await seedApp(a);
  await assertFails(transition(me('s1'),a,'annullato'));await assertFails(deleteDoc(doc(me('prof'),'appuntamenti','a1')));
});
const ind=()=>({id:'i1',idProfessore:'prof',dataInizio:date,dataFine:date,oraInizio:null,oraFine:null,motivo:'Privato'});
const absence=i=>Object.fromEntries(['idProfessore','dataInizio','dataFine','oraInizio','oraFine'].map(k=>[k,i[k]]));
function addAbsence(actor='prof',version=true,mirror=true) {
  const db=me(actor),b=writeBatch(db),i=ind();b.set(doc(db,'indisponibilita','i1'),i);
  if(mirror)b.set(doc(db,'assenze','i1'),absence(i));
  if(version){const p=person('prof','professore');p.disponibilitaVersione=1;b.update(doc(db,'users','prof'),{disponibilitaVersione:1});b.set(doc(db,'professori','prof'),publicProf(p));}
  return b.commit();
}
test('indisponibilità privata e intervallo consultabile con versione atomica',async()=>{
  await assertSucceeds(addAbsence());
  await assertSucceeds(getDocs(query(collection(me('prof'),'indisponibilita'),where('idProfessore','==','prof'))));
  await assertFails(getDoc(doc(me('s1'),'indisponibilita','i1')));
  await assertSucceeds(getDocs(query(collection(me('s1'),'assenze'),where('idProfessore','==','prof'))));
  assert.equal((await getDoc(doc(me('s1'),'assenze','i1'))).data().motivo,undefined);
  const db=me('prof'),b=writeBatch(db),p=person('prof','professore');p.disponibilitaVersione=2;
  b.delete(doc(db,'indisponibilita','i1'));b.delete(doc(db,'assenze','i1'));b.update(doc(db,'users','prof'),{disponibilitaVersione:2});b.set(doc(db,'professori','prof'),publicProf(p));
  await assertSucceeds(b.commit());
});
test('indisponibilità senza versione, senza copia o da studente negata',async()=>{
  await assertFails(addAbsence('prof',false));await assertFails(addAbsence('prof',true,false));await assertFails(addAbsence('s1'));
});
test('sottocollezioni e percorsi sconosciuti negati',async()=>{
  await assertFails(setDoc(doc(me('s1'),'users','s1','secrets','x'),{value:'x'}));
  await assertFails(setDoc(doc(me('s1'),'unknown','x'),{uid:'s1'}));
});
