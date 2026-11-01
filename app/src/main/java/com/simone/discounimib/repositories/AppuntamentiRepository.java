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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import com.simone.discounimib.domain.RegoleAppuntamento;
import com.simone.discounimib.utils.StatoRichiesta;
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

        Disponibilita disponibilita(String data) { return disponibilita(data, null); }

        Disponibilita disponibilita(String data, String escluso) {
            Professore prof = profilo.toObject(Professore.class);
            if (prof == null || !"professore".equals(prof.getRuolo())) {
                throw new IllegalArgumentException("Profilo docente non disponibile");
            }
            List<Appuntamento> occupati = new ArrayList<>();
            for (Appuntamento a : appuntamenti) if (!Objects.equals(escluso, a.getId())) occupati.add(a);
            return new Disponibilita(prof, DisponibilitaMapper.slots(prof, data, indisponibilita, occupati));
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
        db.collection("professori").document(profId).get(Source.SERVER).addOnSuccessListener(profilo -> {
            if (!profilo.exists()) {
                callback.onError("Profilo docente non disponibile");
                return;
            }
            Task<QuerySnapshot> indisponibilita = db.collection("assenze")
                    .whereEqualTo("idProfessore", profId).get(Source.SERVER);
            Task<QuerySnapshot> appuntamenti = db.collection("occupazioni")
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
                    app.setInizioIstante(DatiPubblici.istante(app.getData(), app.getOraInizio()));
                    app.setFineIstante(DatiPubblici.istante(app.getData(), app.getOraFine()));
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
                        DocumentReference blocco = db.collection("slotPrenotati").document(DatiPubblici.chiaveSlot(app));
                        DocumentSnapshot occupato = transaction.get(blocco);
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
                        if (esistente.exists() || occupato.exists()) {
                            throw new FirebaseFirestoreException("Slot appena prenotato. Ricarica gli orari.",
                                    FirebaseFirestoreException.Code.ALREADY_EXISTS);
                        }
                        // Ricontrolla anche il trascorrere del tempo durante la richiesta.
                        if (!dati.disponibilita(app.getData()).slots.contains(app.getOraInizio())) {
                            throw new FirebaseFirestoreException("L'orario non è più prenotabile.",
                                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }
                        app.setNomeStudente(studente.getString("nome") + " " + studente.getString("cognome"));
                        app.setEmailStudente(studente.getString("email"));
                        transaction.set(ref, app);
                        transaction.set(db.collection("occupazioni").document(ref.getId()), DatiPubblici.occupazione(app));
                        transaction.set(blocco, DatiPubblici.blocco(app));
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

    public LiveData<StatoRichiesta<Appuntamento>> getAppuntamento(String id) {
        return new MutableLiveData<StatoRichiesta<Appuntamento>>() {
            private ListenerRegistration listener;
            @Override protected void onActive() {
                setValue(StatoRichiesta.caricamento());
                if (id == null || id.isEmpty()) {
                    setValue(StatoRichiesta.errore("Appuntamento non selezionato"));
                    return;
                }
                listener = db.collection("appuntamenti").document(id).addSnapshotListener((doc, error) -> {
                    try {
                        if (error != null || doc == null || !doc.exists()) {
                            setValue(StatoRichiesta.errore("Impossibile caricare l'appuntamento"));
                            return;
                        }
                        Appuntamento app = doc.toObject(Appuntamento.class);
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (app == null || user == null || (!user.getUid().equals(app.getIdProfessore())
                                && !user.getUid().equals(app.getIdStudente()))) {
                            setValue(StatoRichiesta.errore("Appuntamento non autorizzato"));
                            return;
                        }
                        app.setId(doc.getId());
                        setValue(StatoRichiesta.successo(app));
                    } catch (RuntimeException e) {
                        setValue(StatoRichiesta.errore("Dati dell'appuntamento non validi"));
                    }
                });
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
            }
        };
    }

    private static boolean futuro(Appuntamento app) {
        return RegoleAppuntamento.futuro(app.getData(), app.getOraInizio(),
                LocalDateTime.now(CalcolatoreDisponibilita.FUSO));
    }

    private static long revisione(DocumentSnapshot doc) {
        Long valore = doc.getLong("revisione");
        return valore == null ? 0 : valore;
    }

    private static Map<String, Object> aggiornamento(String stato, long versione) {
        Map<String, Object> valori = new HashMap<>();
        valori.put("stato", stato);
        valori.put("revisione", versione);
        valori.put("statoSincronizzazione", "in_attesa");
        valori.put("erroreSincronizzazione", null);
        return valori;
    }

    public void updateAppuntamentoStato(String id, String nuovoStato, OnTaskCompleted callback) {
        FirebaseUser utente = FirebaseAuth.getInstance().getCurrentUser();
        if (utente == null || id == null || id.isEmpty()) {
            callback.onError("Sessione o appuntamento non validi");
            return;
        }
        DocumentReference ref = db.collection("appuntamenti").document(id);
        db.runTransaction(transaction -> {
            DocumentSnapshot documento = transaction.get(ref);
            Appuntamento app = documento.toObject(Appuntamento.class);
            if (app == null) throw new IllegalArgumentException("Appuntamento non trovato");
            app.setId(id);
            DocumentReference blocco = db.collection("slotPrenotati").document(DatiPubblici.chiaveSlot(app));
            DocumentSnapshot occupato = transaction.get(blocco);
            RegoleAppuntamento.valida(app.getStato(), nuovoStato, utente.getUid(),
                    app.getIdProfessore(), app.getIdStudente(), futuro(app));
            transaction.update(ref, aggiornamento(nuovoStato, revisione(documento) + 1));
            app.setStato(nuovoStato);
            transaction.set(db.collection("occupazioni").document(id), DatiPubblici.occupazione(app));
            if (!DisponibilitaMapper.occupaSlot(nuovoStato) && id.equals(occupato.getString("appuntamentoId"))) {
                transaction.delete(blocco);
            }
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(messaggio(e)));
    }

    public void caricaDisponibilitaRiprogrammazione(String id, String data, OnDisponibilita callback) {
        leggiPerModifica(id, new OnDocumento() {
            @Override public void onSuccess(DocumentSnapshot doc, FirebaseUser user) {
                Appuntamento app = doc.toObject(Appuntamento.class);
                caricaDati(app.getIdProfessore(), new OnDati() {
                    @Override public void onSuccess(DatiPrenotazione dati) {
                        try {
                            Disponibilita risultato = dati.disponibilita(data, id);
                            List<String> orari = new ArrayList<>(risultato.slots);
                            if (Objects.equals(data, app.getData())) orari.remove(app.getOraInizio());
                            callback.onSuccess(new Disponibilita(risultato.professore, orari));
                        } catch (RuntimeException e) { callback.onError("Disponibilità non valida"); }
                    }
                    @Override public void onError(String message) { callback.onError(message); }
                });
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    private interface OnDocumento {
        void onSuccess(DocumentSnapshot doc, FirebaseUser user);
        void onError(String message);
    }

    private void leggiPerModifica(String id, OnDocumento callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || id == null || id.isEmpty()) {
            callback.onError("Sessione o appuntamento non validi");
            return;
        }
        db.collection("appuntamenti").document(id).get(Source.SERVER).addOnSuccessListener(doc -> {
            try {
                Appuntamento app = doc.toObject(Appuntamento.class);
                if (app == null) throw new IllegalArgumentException("Appuntamento non trovato");
                RegoleAppuntamento.valida(app.getStato(), "riprogramma", user.getUid(),
                        app.getIdProfessore(), app.getIdStudente(), futuro(app));
                callback.onSuccess(doc, user);
            } catch (RuntimeException e) { callback.onError(messaggio(e)); }
        }).addOnFailureListener(e -> callback.onError(messaggio(e)));
    }

    /** Nuova richiesta collegata; annullamento del precedente e prenotazione sono un'unica transazione. */
    public void riprogramma(String id, String data, String ora, OnRiprogramma callback) {
        leggiPerModifica(id, new OnDocumento() {
            @Override public void onSuccess(DocumentSnapshot originale, FirebaseUser user) {
                Appuntamento vecchio = originale.toObject(Appuntamento.class);
                if (Objects.equals(data, vecchio.getData()) && Objects.equals(ora, vecchio.getOraInizio())) {
                    callback.onError("Scegli un orario diverso");
                    return;
                }
                caricaDati(vecchio.getIdProfessore(), new OnDati() {
                    @Override public void onSuccess(DatiPrenotazione dati) {
                        try {
                            Disponibilita disponibilita = dati.disponibilita(data, id);
                            if (!disponibilita.slots.contains(ora)) {
                                callback.onError("Il nuovo orario non è più disponibile");
                                return;
                            }
                            long tentativo = dati.appuntamenti.stream().filter(a ->
                                    data.equals(a.getData()) && ora.equals(a.getOraInizio())).count();
                            DocumentReference nuovaRef = db.collection("appuntamenti")
                                    .document(idSlot(vecchio.getIdProfessore(), data, ora) + "-" + tentativo);
                            Appuntamento nuovo = new Appuntamento();
                            nuovo.setId(nuovaRef.getId());
                            nuovo.setIdProfessore(vecchio.getIdProfessore());
                            nuovo.setIdStudente(vecchio.getIdStudente());
                            nuovo.setNomeProfessore(vecchio.getNomeProfessore());
                            nuovo.setNomeStudente(vecchio.getNomeStudente());
                            nuovo.setEmailProfessore(vecchio.getEmailProfessore());
                            nuovo.setEmailStudente(vecchio.getEmailStudente());
                            nuovo.setMotivazione(vecchio.getMotivazione());
                            nuovo.setData(data);
                            nuovo.setOraInizio(ora);
                            nuovo.setOraFine(LocalTime.parse(ora, CalcolatoreDisponibilita.ORA)
                                    .plusMinutes(disponibilita.professore.getDurataAppuntamento())
                                    .format(CalcolatoreDisponibilita.ORA));
                            nuovo.setInizioIstante(DatiPubblici.istante(data, nuovo.getOraInizio()));
                            nuovo.setFineIstante(DatiPubblici.istante(data, nuovo.getOraFine()));
                            nuovo.setDataCreazione(com.google.firebase.Timestamp.now());
                            nuovo.setAppuntamentoPrecedenteId(id);
                            boolean docente = user.getUid().equals(vecchio.getIdProfessore());
                            nuovo.setStato(docente ? "accettato" : "sospeso");
                            nuovo.setRevisione(1);
                            nuovo.setSincronizzataRevisione(docente ? 0 : 1);
                            nuovo.setStatoSincronizzazione(docente ? "in_attesa" : "non_richiesta");
                            db.runTransaction(transaction -> {
                                DocumentSnapshot attuale = transaction.get(originale.getReference());
                                DocumentSnapshot profilo = transaction.get(dati.profilo.getReference());
                                DocumentSnapshot destinazione = transaction.get(nuovaRef);
                                DocumentReference nuovoBlocco = db.collection("slotPrenotati")
                                        .document(DatiPubblici.chiaveSlot(nuovo));
                                DocumentSnapshot nuovoOccupato = transaction.get(nuovoBlocco);
                                DocumentReference vecchioBlocco = db.collection("slotPrenotati")
                                        .document(DatiPubblici.chiaveSlot(vecchio));
                                DocumentSnapshot vecchioOccupato = transaction.get(vecchioBlocco);
                                Appuntamento app = attuale.toObject(Appuntamento.class);
                                if (app == null || revisione(attuale) != revisione(originale)
                                        || !Objects.equals(app.getStato(), vecchio.getStato())) {
                                    throw new IllegalArgumentException("L'appuntamento è cambiato. Ricarica i dettagli.");
                                }
                                RegoleAppuntamento.valida(app.getStato(), "riprogramma", user.getUid(),
                                        app.getIdProfessore(), app.getIdStudente(), futuro(app));
                                for (String campo : new String[]{"ruolo", "giornoRicevimento", "oraInizio",
                                        "oraFine", "durataAppuntamento", "disponibilitaVersione"}) {
                                    if (!Objects.equals(profilo.get(campo), dati.profilo.get(campo))) {
                                        throw new IllegalArgumentException("La disponibilità è cambiata. Riprova.");
                                    }
                                }
                                if (destinazione.exists() || nuovoOccupato.exists() || !dati.disponibilita(data, id).slots.contains(ora)) {
                                    throw new IllegalArgumentException("Slot appena occupato. Scegli un altro orario.");
                                }
                                Map<String, Object> modifiche = aggiornamento("annullato", revisione(attuale) + 1);
                                modifiche.put("sostituitoDaId", nuovaRef.getId());
                                transaction.update(originale.getReference(), modifiche);
                                transaction.set(nuovaRef, nuovo);
                                app.setStato("annullato");
                                transaction.set(db.collection("occupazioni").document(id), DatiPubblici.occupazione(app));
                                transaction.set(db.collection("occupazioni").document(nuovaRef.getId()), DatiPubblici.occupazione(nuovo));
                                if (id.equals(vecchioOccupato.getString("appuntamentoId"))) transaction.delete(vecchioBlocco);
                                transaction.set(nuovoBlocco, DatiPubblici.blocco(nuovo));
                                return null;
                            }).addOnSuccessListener(ignored -> callback.onSuccess(nuovaRef.getId()))
                                    .addOnFailureListener(e -> callback.onError(messaggio(e)));
                        } catch (RuntimeException e) { callback.onError("Nuova data o orario non validi"); }
                    }
                    @Override public void onError(String message) { callback.onError(message); }
                });
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void richiediNuovaSincronizzazione(String id, boolean emailVerificata, OnTaskCompleted callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || id == null) {
            callback.onError("Sessione non valida");
            return;
        }
        DocumentReference ref = db.collection("appuntamenti").document(id);
        db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(ref);
            if (!user.getUid().equals(doc.getString("idProfessore"))) {
                throw new IllegalArgumentException("Solo il docente può sincronizzare");
            }
            if ("sospeso".equals(doc.getString("stato"))) return null;
            Long scadenza = doc.getLong("syncLeaseFino");
            if (scadenza != null && scadenza > System.currentTimeMillis()) {
                throw new IllegalArgumentException("Sincronizzazione ancora in corso. Attendi prima di riprovare.");
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("statoSincronizzazione", "in_attesa");
            if (revisione(doc) == 0) updates.put("revisione", 1L);
            updates.put("erroreSincronizzazione", null);
            if (emailVerificata) updates.put("statoEmail", "gestita");
            transaction.update(ref, updates);
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(messaggio(e)));
    }

    public interface OnRiprogramma {
        void onSuccess(String nuovoId);
        void onError(String message);
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
