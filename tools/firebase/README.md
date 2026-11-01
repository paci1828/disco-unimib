# Gestione Firebase di DISCo Calendar

Questi strumenti operano sul progetto `discounimib-2026-db0ad`. Le credenziali restano nella sessione della CLI ufficiale: l'adapter usa Firebase Admin per Authentication e il client Google Cloud per Firestore. Il codice amministrativo è sotto `tools/` e non viene incluso nell'APK.

## Installazione e test

Dalla radice del progetto:

```powershell
npm ci --prefix tools/firebase --ignore-scripts
npm --prefix tools/firebase run test:rules
```

Servono Node.js recente (verificato con 24) e Java 21 o superiore. Il launcher cerca il runtime dell'Android Studio in esecuzione e le installazioni standard; è possibile indicare un JDK tramite `JAVA_HOME` o `STUDIO_JDK`. Le variabili vengono applicate al processo dei test.

I test usano esclusivamente il progetto dimostrativo `demo-discounimib` e l'emulatore locale sulla porta 8089.

## Account e profili

```powershell
npm --prefix tools/firebase run profiles:list
```

L'elenco comprende UID, email, nome Authentication e presenza/ruolo del profilo Firestore; le password non vengono esportate.

Per creare o completare i profili, preparare `tools/firebase/profiles.local.json` con `projectId` uguale al progetto sopra e una lista `profiles`. Il file è escluso da Git.

Per un account docente esistente con email non verificata, indicare esplicitamente l'UID Authentication nel manifest. Il piano segnala l'UID necessario: questo evita di assegnare il ruolo docente basandosi solo su un indirizzo non verificato.

Ogni elemento richiede `email`, `nome`, `cognome`, `ruolo` (`studente` o `professore`); accetta inoltre `uid` per verificare un account già noto, `matricola` e `orario`. L'orario del docente contiene `giornoRicevimento`, `oraInizio`, `oraFine`, `durataAppuntamento`. Per un nuovo docente senza orario esplicito il valore iniziale è Lunedì 09:00–12:00, durata 30 minuti.

```powershell
npm --prefix tools/firebase run profiles:plan
npm --prefix tools/firebase run profiles:apply
```

Il piano legge i dati; `apply` riusa l'UID Authentication esistente oppure crea un account con l'email indicata. Per un account appena creato, il titolare imposta la password tramite Recupera password nell'app.

La scrittura di ogni profilo e della sua eventuale copia docente è atomica. La procedura conserva creazione, ultimo accesso e versione degli orari già presenti; incrementa la versione quando cambia un orario esistente. La rimozione del ruolo docente richiede una procedura dedicata, per preservare lo storico.

I docenti vengono abilitati da questo strumento amministrativo o da un amministratore Firebase. La registrazione pubblica dell'app crea esclusivamente studenti.

## Collezioni

| Collezione | Consultazione | Contenuti |
| --- | --- | --- |
| `users` | proprietario | profilo privato, email, matricola, ruolo, orario |
| `professori` | utenti autenticati | nome professionale e orario |
| `appuntamenti` | studente e docente partecipanti | richiesta, motivazione, contatti e sincronizzazione |
| `occupazioni` | utenti autenticati | intervallo e stato senza identità dello studente |
| `slotPrenotati` | utenti autenticati | blocco esclusivo dello stesso inizio di ricevimento |
| `indisponibilita` | docente proprietario | intervallo e motivo privato |
| `assenze` | utenti autenticati | intervallo senza motivo |

Le copie pubbliche sono vincolate ai documenti privati tramite `getAfter()`; l'app le scrive nella stessa transazione. Il worker Google aggiorna i checkpoint del documento privato. Le regole ne riservano la modifica al docente partecipante.

Le prenotazioni includono `inizioIstante` e `fineIstante` (Timestamp immutabili); i testi data/ora sono vincolati al loro formato Europe/Rome. Il blocco canonico usa `UID_docente + '_' + millisecondi_UTC` dell'inizio.

Il blocco canonico impedisce prenotazioni contemporanee con lo stesso inizio, anche usando ID appuntamento diversi. Il calcolo completo delle sovrapposizioni e delle indisponibilità continua nell'app; una validazione autorevole per client modificati richiede un backend di prenotazione. Questo limite è distinto dalla protezione di profili, email e motivazioni.

## Distribuzione

Dopo i test:

```powershell
npx -y firebase-tools@latest deploy --only firestore:rules,firestore:indexes,auth --project discounimib-2026-db0ad --non-interactive
```

Le regole e il nuovo APK devono essere aggiornati insieme perché le letture della disponibilità ora usano le collezioni separate.

I've set up prototype Security Rules to keep the data in Firestore safe. They are designed to be secure for private profiles, participant-only appointments, and atomic availability projections. However, you should review and verify them before broadly sharing your app. If you'd like, I can help you harden these rules.
