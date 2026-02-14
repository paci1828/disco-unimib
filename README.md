# 📅 DISCo Calendar

Sistema di gestione appuntamenti per ricevimenti universitari tra studenti e professori.

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-orange.svg)](https://firebase.google.com/)
[![Java](https://img.shields.io/badge/Language-Java%2011-blue.svg)](https://www.oracle.com/java/)

---

## 📖 Descrizione

**DISCo Calendar** è un'applicazione Android progettata per semplificare la prenotazione degli appuntamenti di ricevimento tra studenti e professori. 

L'applicazione permette agli studenti di visualizzare la disponibilità dei professori, prenotare appuntamenti e ricevere conferme via email. I professori possono gestire i propri orari di ricevimento, accettare o rifiutare le richieste, e integrare gli appuntamenti con Google Calendar.

---

## ✨ Funzionalità Principali

### 👨‍🎓 Studenti
- ✅ Visualizzazione calendario con professori disponibili per data
- ✅ Prenotazione appuntamenti con motivazione
- ✅ Visualizzazione storico appuntamenti (sospesi, accettati, rifiutati)
- ✅ Cancellazione richieste in sospeso
- ✅ Notifiche email di conferma/rifiuto
- ✅ Accesso alle FAQ

### 👨‍🏫 Professori
- ✅ Gestione orari di ricevimento (giorno, ora inizio/fine, durata slot)
- ✅ Visualizzazione appuntamenti della settimana corrente
- ✅ Gestione richieste appuntamenti (accetta/rifiuta)
- ✅ Gestione indisponibilità (periodi di assenza)
- ✅ Integrazione automatica con Google Calendar
- ✅ Invio email automatiche agli studenti
- ✅ Visualizzazione storico completo appuntamenti

### 🔧 Funzionalità Comuni
- 🔐 Autenticazione Firebase (email/password)
- 💾 Salvataggio credenziali (opzionale)
- ⚙️ Sezione impostazioni personalizzate
- 📧 Contatto sviluppatori via email
- ❓ Sezione FAQ integrata

---

## 🛠️ Tecnologie Utilizzate

### Core
- **Linguaggio**: Java 11
- **IDE**: Android Studio 
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 35)

### Backend & Database
- **Firebase Authentication** - Autenticazione utenti
- **Firebase Firestore** - Database NoSQL in tempo reale
- **Firebase Storage** - Archiviazione file (opzionale)

### Google APIs
- **Google Calendar API** - Integrazione calendario
- **Gmail API** - Invio email automatiche
- **Google Sign-In** - Autenticazione OAuth 2.0

### Librerie Android
- **AndroidX Libraries** - Componenti moderni Android
- **Material Design Components** - UI/UX Material Design 3
- **Lifecycle (ViewModel + LiveData)** - Architettura MVVM
- **RecyclerView & CardView** - Liste e visualizzazioni
- **Multidex** - Gestione limite metodi 64K

### Altre Dipendenze
- **gRPC** - Comunicazione Firebase-Firestore
- **JavaMail API** - Gestione email avanzata
- **Gson** - Parsing JSON

---

## 🏗️ Architettura

L'applicazione segue il pattern architetturale **MVVM (Model-View-ViewModel)** per separare la logica di business dall'UI.

```
com.simone.discounimib/
├── activities/           # Activity principali
│   ├── MainActivity      # Splash screen
│   ├── LoginActivity     # Autenticazione
│   └── HomeActivity      # Container fragment
├── fragments/            # Fragment UI
│   ├── professore/       # Fragment specifici professore
│   └── studente/         # Fragment specifici studente
├── adapters/             # Adapter RecyclerView
├── models/               # POJO/Data classes
├── viewmodels/           # ViewModel (logica UI)
├── repositories/         # Repository (accesso dati)
├── utils/                # Utility classes
└── services/             # Servizi Google APIs
```

### Componenti Chiave

**Activity:**
- `MainActivity` - Splash screen con controllo credenziali salvate
- `LoginActivity` - Login con Firebase Authentication
- `HomeActivity` - Navigation drawer/bottom nav con gestione fragment

**Fragment Professore:**
- `HomeFragment` - Appuntamenti settimana corrente
- `ElencoAppuntamentiSospesiFragment` - Richieste da gestire
- `AccettaRifiutaAppuntamentoFragment` - Dettaglio e decisione
- `ElencoIndisponibilitaFragment` - Gestione periodi assenza
- `ImpostazioniRicevimentoFragment` - Modifica orari

**Fragment Studente:**
- `PrenotaAppuntamentoCalendarioFragment` - Selezione data e professore
- `PrenotaAppuntamentoFragment` - Form prenotazione
- `ElencoAppuntamentiFragment` - Storico personale

**Fragment Comuni:**
- `VisualizzaAppuntamentoFragment` - Dettaglio appuntamento
- `ImpostazioniFragment` - Preferenze utente
- `ImpostazioniFaqFragment` - Domande frequenti
- `ImpostazioniContattaciFragment` - Email sviluppatori



### 2. Configurazione Firebase

#### 2.1 Crea Progetto Firebase

1. Vai su [Firebase Console](https://console.firebase.google.com/)
2. Crea nuovo progetto "DISCo Calendar"
3. Aggiungi app Android con package name: `c
4. Scarica `google-services.json`
5. Posiziona il file in `app/google-services.json`

#### 2.2 Abilita Servizi Firebase

**Authentication:**
- Vai in Authentication → Sign-in method
- Abilita "Email/Password"

**Firestore Database:**
- Vai in Firestore Database → Create database
- Modalità: Production
- Location: europe-west1 (o più vicina)

#### 2.3 Configura Database

Usa lo script di inizializzazione fornito:

```bash
cd firebase-init
npm install
node init-firestore.js
```

Oppure crea manualmente le collection come da documentazione (vedi `/docs/firestore-structure.md`).

#### 2.4 Configura Regole di Sicurezza

Copia le regole da `firestore.rules` nella Firebase Console:

```
Firestore Database → Rules → [incolla regole] → Publish
```

### 3. Configurazione Google APIs

#### 3.1 Google Calendar API

1. Vai su [Google Cloud Console](https://console.cloud.google.com/)
2. Seleziona il progetto Firebase
3. APIs & Services → Library
4. Cerca e abilita "Google Calendar API"
5. Credentials → Create OAuth 2.0 Client ID


#### 3.2 Gmail API

1. Nella stessa console Google Cloud
2. Abilita "Gmail API"
3. Usa le stesse credenziali OAuth 2.0

### 4. Build Progetto

#### 4.1 Apri in Android Studio

```bash
File → Open → [seleziona cartella progetto]
```

#### 4.2 Sync Gradle

```bash
File → Sync Project with Gradle Files
```

#### 4.3 Build

```bash
Build → Clean Project
Build → Rebuild Project
```

### 5. Esegui Applicazione

```bash
Run → Run 'app'
```

Oppure usa shortcut: `Shift + F10`

---


