# DISCo Calendar

App Android Java/XML per i ricevimenti universitari.

Il calcolo degli slot è in domain/CalcolatoreDisponibilita ed è indipendente da Android e Firebase.
DisponibilitaMapper traduce i dati salvati in intervalli. PrenotazioneViewModel gestisce
caricamento, errori, prenotazione e ripristino di slot e motivazione.

## Prenotazioni

- Giorno settimanale, intera durata dello slot, appuntamenti esistenti e indisponibilità
  vengono verificati prima del salvataggio; i dati devono arrivare dal server.
- Le indisponibilità da dataInizio/oraInizio a dataFine/oraFine sono intervalli continui.
  Entrambi gli orari vuoti o assenti indicano giornate intere.
- Sospesi e accettati occupano lo slot; rifiutati e annullati lo liberano.
- Le nuove richieste hanno un ID deterministico per docente, data, ora iniziale e numero
  di richieste precedenti per quello slot. Una transazione impedisce che due client
  aggiornati creino lo stesso tentativo. Lo storico dei rifiuti viene conservato.
- Non eliminare direttamente i documenti degli appuntamenti: il protocollo conta lo storico.
- Inizio e fine sono memorizzati anche come Timestamp (`inizioIstante`, `fineIstante`).
  Le regole verificano che data e ora visualizzate corrispondano agli istanti in Europe/Rome.
- Modifiche a orari e indisponibilità incrementano atomicamente il campo
  users/{uid}.disponibilitaVersione e la relativa copia in professori/{uid}.
  Il bootstrap inizializza la versione a zero. La prenotazione controlla versione e configurazione.

## Profili e regole Firebase

Le regole sono in `firestore.rules`; schema, bootstrap e comandi sono descritti in
[FIREBASE_SETUP.md](FIREBASE_SETUP.md) e [tools/firebase/README.md](tools/firebase/README.md).

`users` contiene i profili privati. La directory `professori` contiene soltanto nomi
professionali e orari. La disponibilità si calcola da `occupazioni` e `assenze`,
senza leggere email e motivazioni degli altri utenti.
I documenti privati degli appuntamenti sono accessibili ai due partecipanti;
le indisponibilità con motivo sono riservate al docente.

Le copie pubbliche sono aggiornate nelle stesse transazioni dei dati privati.
`slotPrenotati` impedisce due prenotazioni attive con lo stesso inizio, anche usando
ID diversi. Il controllo completo di sovrapposizioni a inizi diversi e delle
indisponibilità resta nell'app e richiede un backend fidato per client modificati.

La registrazione pubblica crea studenti. Il ruolo docente è assegnato da un
amministratore e non è modificabile dai client. La sessione richiede Firebase Auth
e una cache con UID corrispondente. Il logout disconnette Firebase e Google.

## Verifica locale

Da PowerShell nella cartella del progetto:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

I test locali verificano intervalli, durate, date, stati, cambi d'ora e proiezioni senza dati privati.
I test delle regole si eseguono sull'emulatore:

```powershell
npm ci --prefix tools/firebase --ignore-scripts
npm --prefix tools/firebase run test:rules
```

## Annullamento e riprogrammazione

Studente e docente aprono il dettaglio dalla propria lista per annullare o riprogrammare
un appuntamento futuro sospeso o accettato. La conferma è esplicita.
La riprogrammazione crea una nuova prenotazione collegata e annulla la precedente nella
stessa transazione: se il nuovo slot è occupato, non viene modificata la vecchia prenotazione.
Lo storico è navigabile in entrambe le direzioni.
La richiesta dello studente torna sospesa; il docente conferma direttamente il nuovo orario.
Il nuovo evento Calendar attende che la rimozione di quello precedente sia stata registrata.

## Sincronizzazione Google

Dal dettaglio il docente seleziona **Collega / riprova Google** e autorizza Calendar Events
e Gmail Send, con l'account Google corrispondente alla propria email Firebase presente sul dispositivo.
L'abilitazione è facoltativa e separata dalla conferma della prenotazione.
Calendar API e Gmail API devono essere abilitate nel progetto Google Cloud e il client
Android OAuth deve contenere package e SHA-1 corretti; consenso e utenti di test vanno configurati.

WorkManager gestisce un lavoro immediato e un recupero periodico (intervallo minimo 15 minuti,
esecuzione effettiva soggetta a rete e vincoli Android). Il lavoro continua fuori dal Fragment.
Il logout cancella i lavori del relativo UID. Ogni esecuzione controlla sessione e ruolo docente.
Gli aggiornamenti dello studente restano in attesa finché un dispositivo autorizzato del docente
può elaborarli: non vengono usate le credenziali dello studente per modificare il calendario del docente.
Per notifiche immediate anche senza un dispositivo docente attivo è necessario un backend.

Il documento appuntamento contiene l'outbox: revisione, sincronizzataRevisione,
calendarioRevisione, emailRevisione, statoSincronizzazione, statoEmail e relativo errore.
Una lease con token e scadenza coordina le esecuzioni concorrenti; le scritture di checkpoint
controllano la revisione e la proprietà della lease. La lease scade dopo un'interruzione.
Le regole backend devono consentire al solo docente la gestione dei checkpoint e impedire
modifiche arbitrarie di partecipanti, transizioni, revisioni e collegamenti di riprogrammazione.

Calendar usa un ID deterministico, date ISO con offset Europe/Rome e durata reale.
Creazione, aggiornamento e rimozione possono essere ritentati; 404/410 in cancellazione
indicano un evento già assente. Gli eventi legacy creati senza averne salvato l'ID non sono
riconoscibili automaticamente e richiedono una verifica manuale prima della migrazione.

Gmail non viene trattato come un servizio con invio idempotente. Prima della chiamata
viene salvato il checkpoint **in_invio**. Se la risposta è persa, oppure il processo termina
prima del checkpoint finale, il tentativo diventa **da_verificare** e non viene reinviato.
Il docente verifica la posta inviata e seleziona **Ho verificato la posta inviata** per
confermare di aver gestito la comunicazione, anche manualmente. Non è una promessa di
consegna esattamente una volta: è una scelta conservativa che evita reinvii automatici
dall'esito incerto. Un rifiuto HTTP 4xx esplicito può essere ritentato dopo aver risolto la causa.
Le email di annullamento dovute a sostituzione vengono omesse: la nuova conferma comunica il nuovo orario.

I test locali includono transizioni e autorizzazioni, ora legale/solare, checkpoint,
risposte perse, revisioni superate e blocco dei reinvii. Non inviano email reali.
Il collaudo end-to-end con due client Firebase e un account Google autorizzato resta necessario;
i test dell'emulatore non esercitano le API Google reali.

Riferimenti: [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work),
[AuthorizationClient](https://developer.android.com/identity/authorization),
[ID evento Calendar](https://developers.google.com/workspace/calendar/api/guides/create-events).

Interventi successivi: backend di disponibilità e controlli server, notifiche e impostazioni.

## Diagnostica modifica orario

Il form distingue caricamento, salvataggio, successo ed errore e conserva i valori
con SavedStateHandle. Un errore di lettura presenta un messaggio persistente e
**Riprova caricamento**; non viene trattato come un campo mancante.
Dopo il salvataggio la conferma resta visibile e il form torna utilizzabile,
anche quando la schermata è stata aperta dal drawer senza una voce nel back stack.

Se il log riporta PERMISSION_DENIED su users/{uid}, compilare i campi non risolve il blocco:
occorre verificare sessione e regole Firestore pubblicate per il progetto configurato.
Il docente autenticato deve poter leggere il proprio profilo e aggiornare i soli campi
di ricevimento e disponibilitaVersione. La versione è aggiornata in transazione,
quindi il relativo documento deve essere leggibile. Non usare regole globali permissive
o rimuovere la transazione per aggirare l'errore.
Il salvataggio pubblica atomicamente anche la copia in professori/{uid}. Le regole del
repository verificano proprietà, ruolo, tipi, versione e corrispondenza della copia.
