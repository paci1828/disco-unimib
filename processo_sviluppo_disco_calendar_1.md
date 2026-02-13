# Processo di Sviluppo Applicazione DISCo Calendar
## Android Studio + Java + Firebase

---

## 1. SETUP INIZIALE DEL PROGETTO

### 1.1 Creazione Progetto Android Studio
- Aprire Android Studio e creare nuovo progetto
- Selezionare template "Empty Activity"
- Configurare:
  - Nome applicazione: DISCo Calendar
  - Package name: com.msm.discocalendar
  - Linguaggio: Java
  - Minimum SDK: API 24 (Android 7.0)
  - Build configuration language: Groovy DSL

### 1.2 Configurazione Firebase
- Accedere alla Firebase Console
- Creare nuovo progetto Firebase "DISCo Calendar"
- Aggiungere l'app Android al progetto Firebase inserendo il package name
- Scaricare il file `google-services.json` e inserirlo nella cartella `app/`
- Aggiungere le dipendenze Firebase nel `build.gradle` (progetto e modulo)

### 1.3 Dipendenze Necessarie
Aggiungere al file `build.gradle` (Module: app):
- Firebase Authentication
- Firebase Firestore
- Firebase Storage (opzionale per immagini profilo)
- Google Play Services Auth (per OAuth 2.0)
- Google Calendar API
- Gmail API
- Material Design Components
- RecyclerView
- CardView
- ViewModel e LiveData (Architecture Components)

---

## 2. CONFIGURAZIONE FIREBASE

### 2.1 Firebase Authentication
- Abilitare nella console Firebase i metodi di autenticazione:
  - Email/Password
  - Google Sign-In (opzionale)
- Configurare le regole di sicurezza base

### 2.2 Firebase Firestore - Struttura Database

#### Collections principali:

**users**
- Documento per ogni utente (UID come chiave)
- Campi: email, nome, cognome, ruolo (studente/professore), matricola
- Sottocollection: (nessuna)

**professori**
- Documento per ogni professore (UID come chiave)
- Campi: giornoRicevimento, oraInizio, oraFine, durataAppuntamento
- Sottocollection: indisponibilita, appuntamenti

**appuntamenti**
- Collection globale di tutti gli appuntamenti
- Campi: idProfessore, idStudente, data, oraInizio, oraFine, motivazione, stato (sospeso/accettato/rifiutato), dataCreazione, emailProfessore, emailStudente, nomeProfessore, nomeStudente

**indisponibilita**
- Collection globale delle indisponibilità
- Campi: idProfessore, dataInizio, dataFine, oraInizio, oraFine, motivo

**faq**
- Collection delle domande frequenti
- Campi: domanda, risposta, ordine

### 2.3 Regole di Sicurezza Firestore
Configurare le rules per:
- Gli studenti possono leggere solo i propri appuntamenti e i dati dei professori
- I professori possono leggere/modificare i propri dati e gestire i propri appuntamenti
- Validazione dei dati in scrittura
- Autenticazione obbligatoria per tutte le operazioni

---

## 3. CONFIGURAZIONE GOOGLE APIs

### 3.1 Google Calendar API
- Abilitare Google Calendar API nella Google Cloud Console
- Configurare OAuth 2.0 consent screen
- Creare credenziali OAuth 2.0 per Android
- Aggiungere gli scope necessari:
  - calendar.events (per creare/eliminare eventi)
  - calendar.readonly (per leggere eventi)

### 3.2 Gmail API
- Abilitare Gmail API nella Google Cloud Console
- Aggiungere scope:
  - gmail.send (per inviare email)
- Configurare le stesse credenziali OAuth 2.0

### 3.3 Gestione OAuth 2.0
- Implementare il flusso di autenticazione OAuth 2.0
- Gestire l'account Google selezionato dall'utente
- Salvare i token di accesso in modo sicuro (SharedPreferences criptate)
- Implementare il refresh automatico dei token

---

## 4. STRUTTURA DELL'APPLICAZIONE

### 4.1 Package Organization
Organizzare il codice in package:
```
com.msm.discocalendar/
├── activities/
│   ├── MainActivity
│   ├── LoginActivity
│   └── HomeActivity
├── fragments/
│   ├── professore/
│   └── studente/
├── adapters/
├── models/
├── viewmodels/
├── repositories/
├── utils/
└── services/
```

### 4.2 Pattern Architetturale
Utilizzare MVVM (Model-View-ViewModel):
- **Model**: classi POJO che rappresentano i dati
- **View**: Activity e Fragment
- **ViewModel**: gestione stato UI e logica di business
- **Repository**: astrazione per l'accesso ai dati

---

## 5. IMPLEMENTAZIONE COMPONENTI CORE

### 5.1 MainActivity (Splash Screen)
- Layout con logo e nome applicazione
- Verificare se l'utente ha salvato le credenziali (SharedPreferences)
- Se salvate: recuperare i dati e navigare a HomeActivity
- Altrimenti: navigare a LoginActivity
- Timer di 2-3 secondi per mostrare lo splash

### 5.2 LoginActivity
- Layout con campi: email, password
- Checkbox "Ricorda credenziali"
- Pulsante "Accedi"
- Link "Password dimenticata"

**Funzionalità:**
- Validazione input (email formato corretto, password non vuota)
- Autenticazione con Firebase Authentication
- Se successo: recuperare dati utente da Firestore
- Verificare il ruolo (studente/professore)
- Salvare credenziali se checkbox selezionato
- Navigare a HomeActivity passando il ruolo utente

### 5.3 HomeActivity
- Implementare Navigation Drawer o Bottom Navigation
- Gestire i Fragment in base al ruolo utente
- Menu specifici per professore e studente
- Toolbar con titolo dinamico
- Gestione del back press

**Menu Professore:**
- Home (appuntamenti settimana)
- Appuntamenti sospesi
- Tutti gli appuntamenti
- Gestione indisponibilità
- Impostazioni
- Logout

**Menu Studente:**
- Prenota appuntamento
- I miei appuntamenti
- Impostazioni
- Logout

---

## 6. IMPLEMENTAZIONE FRAGMENT - PROFESSORE

### 6.1 HomeFragment (Professore)
- RecyclerView con CardView per appuntamenti della settimana corrente
- Query Firestore: appuntamenti dove idProfessore = UID corrente, stato = accettato, data tra lunedì e domenica correnti
- Ordinamento per data e ora
- Visualizzare: data, ora, nome studente, motivazione
- Click su item: navigare a VisualizzaAppuntamentoFragment

**ViewModel:**
- LiveData<List<Appuntamento>> per osservare i dati
- Repository per query Firestore
- Gestione stati: caricamento, successo, errore

### 6.2 ElencoAppuntamentiSospesiFragment
- RecyclerView con appuntamenti in stato "sospeso"
- Query: idProfessore = UID corrente, stato = sospeso
- Ordinamento per data creazione
- Click su item: navigare a AccettaRifiutaAppuntamentoFragment

### 6.3 AccettaRifiutaAppuntamentoFragment
- Mostrare dettagli appuntamento
- Pulsante "Accetta"
- Pulsante "Rifiuta"

**Logica Accettazione:**
1. Verificare che l'orario sia disponibile (no sovrapposizioni con altri appuntamenti accettati)
2. Verificare che non ci siano indisponibilità nello stesso orario
3. Aggiornare stato appuntamento in Firestore a "accettato"
4. Creare evento su Google Calendar via API
5. Inviare email di conferma allo studente via Gmail API
6. Tornare alla lista appuntamenti sospesi

**Logica Rifiuto:**
1. Eliminare appuntamento da Firestore
2. (Opzionale) Inviare email di notifica allo studente
3. Tornare alla lista

### 6.4 ElencoAppuntamentiFragment (Professore)
- RecyclerView con tutti gli appuntamenti (accettati e passati)
- Query: idProfessore = UID corrente, stato = accettato
- Filtri: futuri/passati
- Ordinamento per data
- Long click su item: dialog per eliminare appuntamento (elimina anche da Google Calendar)

### 6.5 ElencoIndisponibilitaFragment
- RecyclerView con CardView per indisponibilità
- Query: idProfessore = UID corrente
- Ordinamento per data inizio
- FAB (Floating Action Button) per aggiungere nuova indisponibilità
- Click su item: navigare a ModificaIndisponibilitaFragment
- Long click: eliminare indisponibilità

### 6.6 AggiungiIndisponibilitaFragment
- Campi: data inizio, data fine, ora inizio (opzionale), ora fine (opzionale), motivo
- DatePicker per le date
- TimePicker per gli orari

**Validazioni:**
- Data fine >= data inizio
- Se orari specificati: ora fine > ora inizio
- Salvare in Firestore nella collection indisponibilita

### 6.7 ModificaIndisponibilitaFragment
- Simile ad AggiungiIndisponibilitaFragment
- Pre-compilare campi con dati esistenti
- Aggiornare documento Firestore

---

## 7. IMPLEMENTAZIONE FRAGMENT - STUDENTE

### 7.1 PrenotaAppuntamentoCalendarioFragment
- CalendarView per selezionare la data
- RecyclerView che mostra professori disponibili nel giorno selezionato

**Logica:**
1. Quando utente seleziona data, query Firestore su collection "professori"
2. Filtrare professori che ricevono in quel giorno della settimana
3. Escludere professori con indisponibilità nella data selezionata
4. Mostrare lista con nome professore
5. Click su professore: navigare a PrenotaAppuntamentoFragment passando data e idProfessore

### 7.2 PrenotaAppuntamentoFragment
- Visualizzare: data selezionata, nome professore, orari disponibili
- Calcolare slot disponibili basandosi su: ora inizio/fine ricevimento, durata appuntamento, appuntamenti già prenotati, indisponibilità
- Spinner o RecyclerView con orari disponibili
- Campo di testo per motivazione
- Pulsante "Prenota"

**Logica Prenotazione:**
1. Validare che sia selezionato un orario
2. Validare motivazione (non vuota)
3. Creare documento in collection "appuntamenti" con stato = "sospeso"
4. Salvare tutti i dati necessari
5. Mostrare conferma
6. Tornare alla schermata calendario

### 7.3 ElencoAppuntamentiFragment (Studente)
- RecyclerView con appuntamenti dello studente
- Query: idStudente = UID corrente
- Mostrare stato (sospeso/accettato/rifiutato)
- Filtri per stato e data
- Click su item: VisualizzaAppuntamentoFragment
- Long click su appuntamenti sospesi: annullare richiesta

---

## 8. FRAGMENT COMUNI

### 8.1 VisualizzaAppuntamentoFragment
- Mostrare tutti i dettagli: data, ora, professore/studente, motivazione, stato
- Layout diverso in base al ruolo
- Pulsante "Chiudi"
- (Opzionale) Se accettato: link per aprire in Google Calendar

### 8.2 ImpostazioniFragment
- Lista di preferenze (PreferenceFragment o RecyclerView custom)
- Voci comuni: FAQ, Contattaci, Logout
- Voci professore: Modifica ricevimento

### 8.3 ImpostazioniHeaderFragment
- Header con foto profilo (opzionale)
- Email utente
- Nome e cognome
- Ruolo

### 8.4 ImpostazioniRicevimentoFragment (solo Professore)
- Campi: giorno settimana (Spinner), ora inizio, ora fine, durata appuntamento
- Pre-compilati con valori correnti da Firestore
- Pulsante "Salva modifiche"

**Validazioni:**
- Ora fine > ora inizio
- Durata appuntamento deve essere divisore esatto dell'intervallo
- Aggiornare documento professore in Firestore

### 8.5 ImpostazioniContattaciFragment
- Campo email destinatario (pre-compilato con email sviluppatori)
- Campo oggetto
- Campo messaggio
- Pulsante "Invia"
- Utilizzare Gmail API per inviare email dall'account dell'utente

### 8.6 ImpostazioniFaqFragment
- RecyclerView con ExpandableCardView
- Query Firestore per recuperare FAQ
- Ordinamento per campo "ordine"
- Click su item: espande/comprime la risposta

---

## 9. IMPLEMENTAZIONE ADAPTERS

### 9.1 Adapter Base
Ogni adapter deve:
- Estendere RecyclerView.Adapter
- Implementare ViewHolder pattern
- Gestire click listener e long click listener
- Aggiornare dinamicamente i dati (metodo setData)

### 9.2 Adapters da Implementare
- **AppuntamentiAdapter**: per liste di appuntamenti
- **AppuntamentiSospesiAdapter**: specifico per gestire layout appuntamenti sospesi
- **IndisponibilitaAdapter**: per liste indisponibilità
- **ProfessoriAdapter**: per lista professori nel calendario prenotazioni
- **OrariDisponibiliAdapter**: per slot orari disponibili
- **FaqAdapter**: con logica di espansione

**Caratteristiche comuni:**
- Layout con CardView
- Uso di ViewBinding per riferimenti view
- Ottimizzazione con DiffUtil per grandi liste
- Gestione stati vuoti (placeholder)

---

## 10. IMPLEMENTAZIONE VIEWMODELS

### 10.1 Pattern ViewModel
Ogni ViewModel deve:
- Estendere AndroidViewModel
- Esporre LiveData per l'UI
- Non contenere riferimenti a Context, View, Activity
- Usare Repository per operazioni dati
- Gestire stati di caricamento

### 10.2 ViewModels da Implementare

**AppuntamentiViewModel**
- LiveData<List<Appuntamento>> per appuntamenti
- Metodi: caricaAppuntamenti, filtraPerData, filtraPerStato

**AppuntamentiSospesiViewModel**
- LiveData<List<Appuntamento>> per appuntamenti sospesi
- Metodo: caricaAppuntamentiSospesi

**IndisponibilitaViewModel**
- LiveData<List<Indisponibilita>> per indisponibilità
- Metodi: caricaIndisponibilita, aggiungi, modifica, elimina

**ProfessoriViewModel**
- LiveData<List<Professore>> per professori disponibili
- Metodo: caricaProfessoriPerData

**CommonViewModel**
- ViewModel condiviso per operazioni comuni
- Metodi: verificaSovrapposizioneAppuntamenti, verificaIndisponibilita, calcolaSlotDisponibili

---

## 11. IMPLEMENTAZIONE REPOSITORIES

### 11.1 Repository Pattern
Ogni Repository deve:
- Astrarre la sorgente dati (Firestore)
- Fornire metodi sincroni che restituiscono LiveData
- Gestire le query Firestore
- Gestire errori e callback

### 11.2 Repositories da Implementare

**AppuntamentiRepository**
- Metodi CRUD per appuntamenti
- Query per filtraggio (per professore, per studente, per stato)
- Listener in tempo reale per aggiornamenti

**ProfessoriRepository**
- Recupero dati professore
- Aggiornamento dati ricevimento
- Query professori disponibili per giorno

**IndisponibilitaRepository**
- CRUD indisponibilità
- Query per professore e data

**UserRepository**
- Recupero dati utente
- Aggiornamento profilo

**FaqRepository**
- Recupero FAQ da Firestore

### 11.3 Gestione Callback
Implementare interfacce callback per:
- onSuccess
- onError
- onLoading
Utilizzare MutableLiveData per propagare stati

---

## 12. MODELS (POJO)

### 12.1 Classi Model da Creare

**User**
- Campi: uid, email, nome, cognome, ruolo, matricola
- Costruttori vuoti (per Firestore)
- Getter e Setter
- Metodi di utilità

**Professore**
- Campi: uid, giornoRicevimento, oraInizio, oraFine, durataAppuntamento
- Estende User o contiene riferimento

**Appuntamento**
- Campi: id, idProfessore, idStudente, data, oraInizio, oraFine, motivazione, stato, dataCreazione, emailProfessore, emailStudente, nomeProfessore, nomeStudente, eventoGoogleCalendarId
- Costruttori, getter, setter
- Metodi di utilità (es. formattaData, formattaOra)

**Indisponibilita**
- Campi: id, idProfessore, dataInizio, dataFine, oraInizio, oraFine, motivo
- Costruttori, getter, setter

**Faq**
- Campi: id, domanda, risposta, ordine
- Costruttori, getter, setter

---

## 13. UTILITIES E HELPERS

### 13.1 DateTimeUtils
- Metodi per formattazione date e ore
- Conversione tra formati (String, Date, Timestamp)
- Calcolo differenze temporali
- Verifica sovrapposizioni orari

### 13.2 ValidationUtils
- Validazione email
- Validazione campi obbligatori
- Validazione formato orari
- Validazione date

### 13.3 FirebaseUtils
- Helper per operazioni Firebase comuni
- Gestione errori Firebase standardizzata
- Utility per conversioni Firestore

### 13.4 GoogleApiUtils
- Helper per autenticazione OAuth 2.0
- Gestione token
- Verifica permessi

### 13.5 SharedPreferencesManager
- Wrapper per SharedPreferences
- Metodi per salvare/recuperare credenziali
- Encryption/Decryption dei dati sensibili
- Gestione flag "ricorda credenziali"

---

## 14. INTEGRAZIONE GOOGLE CALENDAR API

### 14.1 Setup
- Verificare permessi OAuth 2.0
- Richiedere autorizzazione utente al primo accesso
- Gestire account Google multipli

### 14.2 CalendarService
Classe helper per:

**Creazione Evento:**
- Parametri: titolo, descrizione, data inizio, data fine, email partecipanti
- Usare eventi "primari" del calendario
- Salvare l'ID evento restituito da Google nel campo eventoGoogleCalendarId dell'appuntamento in Firestore
- Gestire timezone correttamente

**Eliminazione Evento:**
- Usare eventoGoogleCalendarId salvato
- Gestire errori se evento già eliminato

**Aggiornamento Evento:**
- Recuperare evento esistente
- Modificare campi
- Aggiornare

### 14.3 Gestione Asincrona
- Utilizzare AsyncTask o Coroutines per chiamate API
- Mostrare ProgressBar durante operazioni
- Gestire errori di rete
- Implementare retry logic

---

## 15. INTEGRAZIONE GMAIL API

### 15.1 Setup
- Stessi prerequisiti OAuth 2.0 di Calendar
- Scope: gmail.send

### 15.2 GmailService
Classe helper per invio email:

**Metodo inviaEmail:**
- Parametri: destinatario, oggetto, corpo messaggio
- Creare messaggio MIME
- Codifica Base64
- Inviare tramite Gmail API
- Gestire allegati (opzionale)

### 15.3 Template Email
Creare template per:
- Email conferma appuntamento studente
- Email appuntamento accettato
- Email appuntamento rifiutato (opzionale)
- Email contatto sviluppatori

Template devono includere:
- Dati appuntamento formattati
- Link utili
- Footer con informazioni applicazione

---

## 16. UI/UX DESIGN

### 16.1 Material Design Guidelines
- Utilizzare Material Components (MaterialButton, MaterialCardView, etc.)
- Palette colori consistente (definire in colors.xml)
- Tipografia coerente (definire in themes.xml)
- Icone Material Design

### 16.2 Layout Responsivi
- Utilizzare ConstraintLayout per flessibilità
- Supportare orientamento portrait e landscape
- Layout alternativi per tablet (layout-large)
- Gestire diverse densità schermo

### 16.3 Feedback Utente
- ProgressBar per operazioni lunghe
- Snackbar per messaggi di conferma/errore
- Dialog per conferme critiche (eliminazioni)
- Animazioni di transizione tra Fragment
- Pull-to-refresh per liste

### 16.4 Accessibilità
- Content descriptions per ImageView
- Dimensioni touch target adeguate (min 48dp)
- Contrasto colori sufficiente
- Supporto TalkBack

---

## 17. GESTIONE ERRORI E LOGGING

### 17.1 Gestione Errori
Implementare:
- Try-catch per eccezioni prevedibili
- Error handling per chiamate Firestore
- Error handling per Google APIs
- Messaggi di errore user-friendly

### 17.2 Logging
- Utilizzare Log.d, Log.e, Log.w appropriatamente
- Tag consistenti per ogni classe
- Non loggare dati sensibili in produzione
- Implementare logging condizionale (BuildConfig.DEBUG)

### 17.3 Crash Reporting
- Integrare Firebase Crashlytics
- Log eccezioni non gestite
- Tracking eventi critici

---

## 18. TESTING

### 18.1 Unit Testing
- Test per ViewModel
- Test per Repository
- Test per Utils e Helper
- Mock Firestore con libreria mockito

### 18.2 Integration Testing
- Test integrazione Firebase
- Test flussi completi (login, prenotazione)

### 18.3 UI Testing
- Espresso per test UI
- Test scenari utente principali
- Test navigation

---

## 19. SICUREZZA

### 19.1 Firestore Security Rules
Regole dettagliate:
```
Studenti possono:
- Leggere collection professori
- Leggere solo propri appuntamenti
- Creare appuntamenti (stato sospeso)
- Eliminare propri appuntamenti sospesi
- Leggere FAQ

Professori possono:
- Leggere/modificare propri dati
- Leggere/modificare appuntamenti dove sono il professore
- Creare/modificare/eliminare proprie indisponibilità
- Leggere FAQ
```

### 19.2 Validazione Input
- Sanitizzare tutti gli input utente
- Validare lato client E lato server (Cloud Functions)
- Prevenire injection attacks

### 19.3 Gestione Credenziali
- Non hardcodare API keys nel codice
- Usare BuildConfig per secrets
- Offuscare con ProGuard in produzione

---

## 20. OTTIMIZZAZIONE E PERFORMANCE

### 20.1 Query Firestore
- Creare indici compositi necessari
- Limitare risultati query (limit)
- Utilizzare pagination per liste lunghe
- Implementare caching con Firestore offline persistence

### 20.2 Gestione Memoria
- Evitare memory leaks (context references in AsyncTask)
- Utilizzare WeakReference dove appropriato
- Liberare risorse nel onDestroy

### 20.3 Network
- Implementare retry con exponential backoff
- Gestire modalità offline
- Comprimere dati quando possibile

---

## 21. BUILD E DEPLOYMENT

### 21.1 Configurazione Build
- Definire buildTypes (debug, release)
- Configurare ProGuard per release
- Versioning (versionCode e versionName)
- Signing configuration

### 21.2 Varianti Build
- Ambiente sviluppo (Firebase dev)
- Ambiente produzione (Firebase prod)

### 21.3 Release
- Generare APK/AAB firmato
- Testing su dispositivi reali multipli
- Upload su Google Play Console (alpha/beta track)

---

## 22. DOCUMENTAZIONE

### 22.1 Code Documentation
- Javadoc per classi e metodi pubblici
- Commenti inline per logica complessa
- README.md del progetto

### 22.2 User Documentation
- Guida utente in-app (FAQ)
- Screenshot per Play Store
- Video demo (opzionale)

---

## 23. MANUTENZIONE E AGGIORNAMENTI

### 23.1 Monitoring
- Firebase Analytics per tracciare utilizzo
- Crashlytics per errori
- Performance monitoring

### 23.2 Aggiornamenti
- Changelog per ogni versione
- Backward compatibility
- Gestione migrazioni database

---

## CHECKLIST FINALE

### Funzionalità Core
- [ ] Login con Firebase Authentication
- [ ] Salvataggio credenziali
- [ ] Logout
- [ ] Professore: visualizza appuntamenti settimana
- [ ] Professore: gestisce appuntamenti sospesi (accetta/rifiuta)
- [ ] Professore: visualizza tutti appuntamenti
- [ ] Professore: gestisce indisponibilità (CRUD)
- [ ] Professore: modifica orari ricevimento
- [ ] Studente: seleziona data e professore
- [ ] Studente: prenota appuntamento
- [ ] Studente: visualizza propri appuntamenti
- [ ] Integrazione Google Calendar (crea/elimina eventi)
- [ ] Integrazione Gmail (invio email conferma)
- [ ] FAQ
- [ ] Contatta sviluppatori

### Aspetti Tecnici
- [ ] MVVM implementato correttamente
- [ ] Repository pattern
- [ ] LiveData per reactive UI
- [ ] Firestore security rules configurate
- [ ] OAuth 2.0 configurato
- [ ] Gestione errori completa
- [ ] UI responsive
- [ ] Testing base implementato
- [ ] Logging appropriato
- [ ] Performance ottimizzate

### Pre-Release
- [ ] Testing completo su dispositivi diversi
- [ ] Verificato su Android 7.0 - 13+
- [ ] ProGuard configurato
- [ ] APK firmato
- [ ] Privacy policy creata
- [ ] Screenshot e descrizione Play Store
- [ ] Beta testing completato

---

## TIMELINE STIMATA

1. **Setup e configurazione** (2-3 giorni)
2. **Autenticazione e struttura base** (3-4 giorni)
3. **Funzionalità professore** (5-7 giorni)
4. **Funzionalità studente** (4-5 giorni)
5. **Integrazione Google APIs** (4-5 giorni)
6. **UI/UX refinement** (3-4 giorni)
7. **Testing e debugging** (4-5 giorni)
8. **Ottimizzazione e documentazione** (2-3 giorni)

**Totale stimato: 27-36 giorni** (dipende dall'esperienza e tempo dedicato)

---

## RISORSE UTILI

### Documentazione
- Firebase Android: https://firebase.google.com/docs/android
- Google Calendar API: https://developers.google.com/calendar
- Gmail API: https://developers.google.com/gmail/api
- Material Design: https://material.io/develop/android

### Librerie Consigliate
- Glide/Picasso: caricamento immagini
- Gson: parsing JSON
- EventBus: comunicazione tra componenti (opzionale)

---

*Documento creato per il progetto DISCo Calendar - Gruppo MSM*
