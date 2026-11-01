package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.models.Professore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AppuntamentiRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static final class Disponibilita {
        public final Professore professore;
        public final List<String> slots;

        Disponibilita(Professore professore, List<String> slots) {
            this.professore = professore;
            this.slots = java.util.Collections.unmodifiableList(new ArrayList<>(slots));
        }
    }

    private static final class DatiPrenotazione {
        final DocumentSnapshot profilo;
        final List<Indisponibilita> indisponibilita;
        final List<Appuntamento> appuntamenti;

        DatiPrenotazione(DocumentSnapshot profilo, QuerySnapshot indisponibilita, QuerySnapshot appuntamenti) {
            this.profilo = profilo;
            this.indisponibilita = indisponibilita.toObjects(Indisponibilita.class);
            this.appuntamenti = leggiAppuntamenti(appuntamenti);
        }

        Disponibilita disponibilita(String data) {
            Professore prof = profilo.toObject(Professore.class);
            if (prof == null || !"professore".equals(prof.getRuolo())) {
                throw new IllegalArgumentException("Profilo docente non disponibile");
            }
            return new Disponibilita(prof, DisponibilitaMapper.slots(prof, data, indisponibilita, appuntamenti));
        }
    }

    public LiveData<List<Appuntamento>> getAppuntamentiPerProfessore(String uidProf, String stato) {
        Query query = db.collection("appuntamenti").whereEqualTo("idProfessore", uidProf);
        if (stato != null) query = query.whereEqualTo("stato", stato);
        return osserva(query);
    }

    public LiveData<List<Appuntamento>> getAppuntamentiPerStudente(String uidStud) {
        return osserva(db.collection("appuntamenti").whereEqualTo("idStudente", uidStud));
    }

    private LiveData<List<Appuntamento>> osserva(Query query) {
        return new MutableLiveData<List<Appuntamento>>() {
            private ListenerRegistration listener;

            @Override protected void onActive() {
                listener = query.addSnapshotListener((value, error) ->
                        setValue(error == null && value != null ? leggiAppuntamenti(value) : null));
            }

            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
            }
        };
    }

    private static List<Appuntamento> leggiAppuntamenti(QuerySnapshot snapshot) {
        List<Appuntamento> risultati = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Appuntamento app = doc.toObject(Appuntamento.class);
            if (app != null) {
                app.setId(doc.getId());
                risultati.add(app);
            }
        }
        return risultati;
    }

    public void caricaDisponibilita(String profId, String data, OnDisponibilita callback) {
        caricaDati(profId, new OnDati() {
            @Override public void onSuccess(DatiPrenotazione dati) {
                try {
                    callback.onSuccess(dati.disponibilita(data));
                } catch (RuntimeException e) {
                    callback.onError("Orari o indisponibilità del docente non validi");
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    private void caricaDati(String profId, OnDati callback) {
        if (profId == null || profId.isEmpty()) {
            callback.onError("Docente non selezionato");
            return;
        }
        // Il profilo precede le query: una variazione della disponibilità invalida la prenotazione.
        db.collection("users").document(profId).get(Source.SERVER).addOnSuccessListener(profilo -> {
            if (!profilo.exists()) {
                callback.onError("Profilo docente non disponibile");
                return;
            }
            Task<QuerySnapshot> indisponibilita = db.collection("indisponibilita")
                    .whereEqualTo("idProfessore", profId).get(Source.SERVER);
            Task<QuerySnapshot> appuntamenti = db.collection("appuntamenti")
                    .whereEqualTo("idProfessore", profId).get(Source.SERVER);
            Tasks.whenAllSuccess(indisponibilita, appuntamenti).addOnSuccessListener(ignored -> {
                try {
                    callback.onSuccess(new DatiPrenotazione(profilo, indisponibilita.getResult(),
                            appuntamenti.getResult()));
                } catch (RuntimeException e) {
                    callback.onError("I dati della disponibilità non sono validi");
                }
            })
                    .addOnFailureListener(e -> callback.onError("Impossibile verificare la disponibilità. Riprova con una connessione attiva."));
        }).addOnFailureListener(e -> callback.onError("Impossibile caricare il docente. Riprova con una connessione attiva."));
    }

    public void creaAppuntamento(Appuntamento app, OnTaskCompleted callback) {
        FirebaseUser utente = FirebaseAuth.getInstance().getCurrentUser();
        if (utente == null || !utente.getUid().equals(app.getIdStudente())) {
            callback.onError("Sessione scaduta. Accedi nuovamente.");
            return;
        }
        if (app.getMotivazione() == null || app.getMotivazione().trim().isEmpty()) {
            callback.onError("Inserisci la motivazione");
            return;
        }
        caricaDati(app.getIdProfessore(), new OnDati() {
            @Override public void onSuccess(DatiPrenotazione dati) {
                try {
                    Disponibilita disponibilita = dati.disponibilita(app.getData());
                    if (!disponibilita.slots.contains(app.getOraInizio())) {
                        callback.onError("L'orario non è più disponibile. Scegli un altro slot.");
                        return;
                    }
                    app.setOraFine(LocalTime.parse(app.getOraInizio(), CalcolatoreDisponibilita.ORA)
                            .plusMinutes(disponibilita.professore.getDurataAppuntamento())
                            .format(CalcolatoreDisponibilita.ORA));
                    app.setStato("sospeso");
                    long tentativo = dati.appuntamenti.stream().filter(a ->
                            app.getData().equals(a.getData()) && app.getOraInizio().equals(a.getOraInizio())).count();
                    // Stesso slot e stessa generazione producono lo stesso documento.
                    // Le richieste rifiutate restano nello storico; il tentativo successivo ha un nuovo ID.
                    DocumentReference ref = db.collection("appuntamenti").document(
                            idSlot(app.getIdProfessore(), app.getData(), app.getOraInizio()) + "-" + tentativo);
                    app.setId(ref.getId());
                    db.runTransaction(transaction -> {
                        DocumentSnapshot corrente = transaction.get(dati.profilo.getReference());
                        DocumentSnapshot esistente = transaction.get(ref);
                        DocumentSnapshot studente = transaction.get(db.collection("users").document(utente.getUid()));
                        if (!"studente".equals(studente.getString("ruolo"))) {
                            throw new FirebaseFirestoreException("Solo uno studente può prenotare",
                                    FirebaseFirestoreException.Code.PERMISSION_DENIED);
                        }
                        String[] campi = {"ruolo", "giornoRicevimento", "oraInizio", "oraFine",
                                "durataAppuntamento", "disponibilitaVersione"};
                        for (String campo : campi) {
                            if (!Objects.equals(corrente.get(campo), dati.profilo.get(campo))) {
                                throw new FirebaseFirestoreException("La disponibilità è cambiata. Ricarica gli orari.",
                                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                            }
                        }
                        if (esistente.exists()) {
                            throw new FirebaseFirestoreException("Slot appena prenotato. Ricarica gli orari.",
                                    FirebaseFirestoreException.Code.ALREADY_EXISTS);
                        }
                        // Ricontrolla anche il trascorrere del tempo durante la richiesta.
                        if (!dati.disponibilita(app.getData()).slots.contains(app.getOraInizio())) {
                            throw new FirebaseFirestoreException("L'orario non è più prenotabile.",
                                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }
                        transaction.set(ref, app);
                        return null;
                    }).addOnSuccessListener(ignored -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(messaggio(e)));
                } catch (RuntimeException e) {
                    callback.onError("Dati di prenotazione non validi. Ricarica gli orari.");
                }
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    private static String idSlot(String profId, String data, String ora) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((profId + "|" + data + "|" + ora).getBytes(StandardCharsets.UTF_8));
            StringBuilder id = new StringBuilder();
            for (byte b : digest) id.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return id.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public void updateAppuntamentoStato(String id, String nuovoStato, OnTaskCompleted callback) {
        if (!"accettato".equals(nuovoStato) && !"rifiutato".equals(nuovoStato)) {
            callback.onError("Esito non valido");
            return;
        }
        FirebaseUser utente = FirebaseAuth.getInstance().getCurrentUser();
        if (utente == null || id == null || id.isEmpty()) {
            callback.onError("Sessione o appuntamento non validi");
            return;
        }
        DocumentReference ref = db.collection("appuntamenti").document(id);
        db.runTransaction(transaction -> {
            DocumentSnapshot documento = transaction.get(ref);
            if (!utente.getUid().equals(documento.getString("idProfessore"))
                    || !"sospeso".equals(documento.getString("stato"))) {
                throw new FirebaseFirestoreException("Appuntamento già gestito o non autorizzato",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            transaction.update(ref, "stato", nuovoStato);
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(messaggio(e)));
    }

    private static String messaggio(Exception e) {
        return e.getMessage() == null ? "Operazione non riuscita. Riprova." : e.getMessage();
    }

    private interface OnDati {
        void onSuccess(DatiPrenotazione dati);
        void onError(String message);
    }

    public interface OnDisponibilita {
        void onSuccess(Disponibilita disponibilita);
        void onError(String message);
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}
