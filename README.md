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
- Modifiche a orari e indisponibilità incrementano atomicamente il campo
  users/{uid}.disponibilitaVersione. L'assenza del campo è compatibile con i profili
  esistenti. La prenotazione controlla in transazione versione e configurazione.

## Backend da verificare prima del rilascio

Non sono state modificate o distribuite regole Firestore.
Il protocollo di concorrenza vale per i client aggiornati; vecchie versioni e scritture
esterne devono essere migrate o vincolate dal backend.

Il controllo degli slot legge gli appuntamenti del docente. Se le regole impediscono
questa lettura allo studente, la prenotazione viene bloccata con un errore.
Non ampliare i permessi sui documenti privati: per il rilascio serve un endpoint o una
proiezione pubblica con soli intervalli occupati, senza motivazioni o dati di altri studenti,
e una prenotazione validata atomicamente lato server.

La registrazione pubblica crea account studente; il ruolo docente va abilitato
amministrativamente e reso non modificabile dal client tramite regole backend.
La sessione richiede Firebase Auth e una cache del profilo con UID corrispondente.
Il logout disconnette Firebase e Google e svuota le preferenze.

## Verifica locale

Da PowerShell nella cartella del progetto:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

I test locali verificano intervalli, durate, date, stati e indisponibilità.
Concorrenza e permessi devono essere collaudati anche con Firebase.

Interventi successivi: backend di disponibilità, sincronizzazione Calendar/Gmail affidabile,
annullamento e riprogrammazione, notifiche e impostazioni.
