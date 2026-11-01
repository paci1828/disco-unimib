# Configurazione Firebase

## Progetto Android

- Project ID: `discounimib-2026-db0ad`.
- Package: `com.simone.discounimib`.
- Firebase Android App ID: `1:522194532599:android:60a744d1197aee627e7bf2`.
- File SDK: `app/google-services.json`, da recuperare dalla app Android di questo progetto.
- Target CLI: `.firebaserc`; specificare comunque `--project discounimib-2026-db0ad` nei comandi di distribuzione.

## Database

Il codice usa Cloud Firestore tramite `FirebaseFirestore.getInstance()`, quindi il database atteso è `(default)`, Standard edition, in `europe-west1`.

L'istanza Realtime Database `discounimib-2026-db0ad-default-rtdb` è un servizio distinto: il codice attuale non la usa.

Il database è stato creato tramite CLI con `deploy --only firestore:indexes`. `firebase.json` e `firestore.indexes.json` descrivono database e indici. Le regole applicative sono ora definite in `firestore.rules`; i test e gli strumenti amministrativi sono in `tools/firebase`.

## Stato della configurazione

- Database verificato sul server: `(default)`, `STANDARD`, `FIRESTORE_NATIVE`, `europe-west1`, realtime updates abilitati.
- App Android registrata e file SDK recuperato tramite CLI e installato nel progetto.
- Build debug e Lint completati con la nuova configurazione, anche in assenza del client OAuth Google.
- Il 1 novembre 2026 la CLI ha confermato la distribuzione di regole e indici Firestore e l'abilitazione di Email/password (`Deploy complete`).
- Verifica locale: 26 test delle regole su emulatore superati; 64 unit test Android, build debug e Lint superati.
- L'[audit delle regole](FIREBASE_RULES_AUDIT.json) descrive i controlli e il limite residuo: la validazione completa di assenze e sovrapposizioni contro client modificati richiede un backend fidato.

- Controllo account via SDK amministrativo dopo l'attivazione: nessun account Authentication presente. I profili reali restano da creare dopo aver ricevuto email, nome, cognome, ruolo e matricola degli studenti.
- La nuova struttura protegge i profili privati e separa directory, assenze e occupazioni consultabili. I profili reali devono essere collegati agli UID del nuovo Authentication tramite lo strumento amministrativo.
- L'accesso Google richiede ancora la configurazione OAuth e un'email di assistenza scelta dal responsabile del progetto.

## Account e profili

Gli account Firebase Authentication e i documenti Firestore del progetto precedente non vengono trasferiti automaticamente nel nuovo progetto.

1. Email/password è già abilitato in Firebase Authentication.
2. Registrare l'utente dall'app collegata al nuovo progetto. La registrazione crea il ruolo `studente`.
3. Per abilitare un docente, usare `tools/firebase/profiles.mjs` con il manifest locale descritto in [tools/firebase/README.md](tools/firebase/README.md). La procedura associa l'email all'UID reale di Authentication e crea sia il profilo privato sia la directory consultabile.
4. Assegnare `ruolo: professore` e configurare i campi di ricevimento riportati sotto.

| Campo | Tipo | Esempio |
| --- | --- | --- |
| `uid` | string | UID Authentication del nuovo progetto |
| `email` | string | email dell'account |
| `nome` | string | nome del docente |
| `cognome` | string | cognome del docente |
| `ruolo` | string | `professore` |
| `matricola` | string | stringa vuota per il docente |
| `dataCreazione` | timestamp | data di creazione |
| `ultimoAccesso` | timestamp | data ultimo accesso |
| `giornoRicevimento` | string | `Lunedì` |
| `oraInizio` | string | `09:00` |
| `oraFine` | string | `12:00` |
| `durataAppuntamento` | integer | `30` |
| `disponibilitaVersione` | integer | `0` all'inizializzazione |

Le collezioni `appuntamenti` e `indisponibilita` vengono popolate dalle rispettive funzioni dell'app. Conservare gli appuntamenti storici: la generazione degli ID di prenotazione li usa per distinguere i tentativi successivi.

## Google

Per l'accesso Google servono il provider abilitato, il client OAuth web del nuovo progetto, le impronte SHA dei certificati Android e una configurazione SDK aggiornata. Il login legge il client ID dalla risorsa generata `default_web_client_id`. Se manca, il pulsante Google viene nascosto e resta il login Email/password. Il file SDK recuperato inizialmente per questo progetto non contiene ancora un client OAuth web.

La sincronizzazione Calendar/Gmail richiede anche le rispettive API, la configurazione della schermata di consenso OAuth e l'autorizzazione del docente sul dispositivo.

## Verifica

Dopo la configurazione dei servizi e delle regole:

1. Compilare l'app con il nuovo file SDK.
2. Accedere con un account del nuovo progetto.
3. Verificare lettura del profilo, salvataggio dell'orario e incremento atomico di `disponibilitaVersione`.
4. Verificare prenotazione, rifiuto, annullamento e riprogrammazione con due account distinti.
5. Verificare che un utente non possa cambiare ruolo, modificare il profilo di altri o leggere appuntamenti altrui.

## Nuove collezioni e test delle regole

Consultare [tools/firebase/README.md](tools/firebase/README.md) per `professori`, `occupazioni`, `slotPrenotati` e `assenze`, le scritture atomiche e i limiti della validazione del calendario contro client modificati. Le proiezioni non contengono email, matricole o motivazioni. L'APK aggiornato usa queste nuove collezioni per la disponibilità.

Le nuove prenotazioni includono `inizioIstante` e `fineIstante` di tipo timestamp. La chiave di `slotPrenotati` usa UID docente e millisecondi UTC dell'inizio. Se viene importato uno storico precedente, questi campi e le copie pubbliche devono essere ricostruiti prima di modificare gli appuntamenti.
