# Analisi per le regole Firestore

Database: `(default)`, Standard, Native, europe-west1, progetto discounimib-2026-db0ad (verificato tramite CLI).

L'app Java/XML legge il proprio profilo `users/{uid}`, elenca i docenti e calcola gli orari da disponibilità e appuntamenti; i partecipanti osservano i propri appuntamenti. Il worker docente aggiorna solo sincronizzazione, revisioni e lease.

Separazione necessaria prima della distribuzione:
- `users`: profilo privato; lettura e modifica del proprietario, ruolo assegnato da amministratore.
- `professori`: nome professionale e orario consultabili da utenti autenticati; copia coerente con il profilo privato.
- `appuntamenti`: motivazione, identità e contatti leggibili solo dai due partecipanti.
- `occupazioni`: intervalli e stato, senza contatti o motivazioni; copia atomica dell'appuntamento.
- `slotPrenotati`: occupazione esclusiva della stessa ora di inizio, collegata all'appuntamento attivo.
- `indisponibilita`: intervalli e motivi privati del docente.
- `assenze`: intervalli consultabili, senza motivo; copia atomica dell'indisponibilità.

Query: docenti per ruolo; assenze/occupazioni per idProfessore; appuntamenti per idStudente o idProfessore (eventuale stato). Nessuna query di studenti sui documenti privati degli altri utenti.

Validazioni previste: proprietà e ruoli immutabili; liste chiuse di campi; tipi e limiti di lunghezza; date reali, tempi e transizioni di stato; controllo revisione; coerenza delle copie con getAfter(); scritture Google riservate al docente.

I profili da ricreare devono essere associati a UID reali del nuovo Authentication; nessuna password verrà inserita nei file del progetto.

Aggiornamento schema: ogni appuntamento contiene `inizioIstante`/`fineIstante` timestamp immutabili. Le regole ricavano data, orari, giorno e durata dagli istanti; il blocco usa il valore UTC canonico. L'involucro iniziale è una mappa esatta con checkpoint nulli/zero o i valori iniziali della riprogrammazione. Gli aggiornamenti consentono soltanto campi di stato o sincronizzazione: l'immutabilità mantiene validi e presenti tutti gli altri campi.
