package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Indisponibilita;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class IndisponibilitaRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Indisponibilita>> getIndisponibilitaPerProfessore(String uidProf) {
        return new MutableLiveData<List<Indisponibilita>>() {
            private ListenerRegistration listener;
            @Override protected void onActive() {
                listener = db.collection("indisponibilita").whereEqualTo("idProfessore", uidProf)
                        .addSnapshotListener((value, error) -> setValue(
                                error == null && value != null ? value.toObjects(Indisponibilita.class) : null));
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
            }
        };
    }

    public void aggiungiIndisponibilita(Indisponibilita ind, OnTaskCompleted callback) {
        try {
            LocalDate da = LocalDate.parse(ind.getDataInizio(), CalcolatoreDisponibilita.DATA);
            LocalDate a = LocalDate.parse(ind.getDataFine(), CalcolatoreDisponibilita.DATA);
            if (a.isBefore(da)) throw new IllegalArgumentException();
            boolean inizioVuoto = ind.getOraInizio() == null || ind.getOraInizio().trim().isEmpty();
            boolean fineVuota = ind.getOraFine() == null || ind.getOraFine().trim().isEmpty();
            if (inizioVuoto != fineVuota) throw new IllegalArgumentException();
            if (inizioVuoto) {
                ind.setOraInizio(null);
                ind.setOraFine(null);
            } else if (!da.atTime(LocalTime.parse(ind.getOraInizio(), CalcolatoreDisponibilita.ORA))
                    .isBefore(a.atTime(LocalTime.parse(ind.getOraFine(), CalcolatoreDisponibilita.ORA)))) {
                throw new IllegalArgumentException();
            }
            if (ind.getIdProfessore() == null || ind.getIdProfessore().isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            callback.onError("Intervallo non valido: controlla date e orari (entrambi vuoti per giornate intere)");
            return;
        }
        DocumentReference ref = db.collection("indisponibilita").document();
        DocumentReference prof = db.collection("users").document(ind.getIdProfessore());
        ind.setId(ref.getId());
        db.runTransaction(transaction -> {
            DocumentSnapshot profilo = transaction.get(prof);
            Long versione = profilo.getLong("disponibilitaVersione");
            Map<String, Object> dati = new java.util.HashMap<>(java.util.Objects.requireNonNull(profilo.getData(), "Profilo non disponibile"));
            dati.put("disponibilitaVersione", versione == null ? 1L : versione + 1);
            transaction.set(ref, ind);
            transaction.set(db.collection("assenze").document(ref.getId()), DatiPubblici.assenza(ind));
            transaction.set(db.collection("professori").document(ind.getIdProfessore()), DatiPubblici.professore(dati));
            transaction.update(prof, "disponibilitaVersione", versione == null ? 1L : versione + 1);
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void eliminaIndisponibilita(String id, OnTaskCompleted callback) {
        DocumentReference ref = db.collection("indisponibilita").document(id);
        db.runTransaction(transaction -> {
            DocumentSnapshot ind = transaction.get(ref);
            String profId = ind.getString("idProfessore");
            if (profId == null) throw new IllegalStateException("Indisponibilità non trovata");
            DocumentReference prof = db.collection("users").document(profId);
            DocumentSnapshot profilo = transaction.get(prof);
            Long versione = profilo.getLong("disponibilitaVersione");
            java.util.Map<String, Object> dati = new java.util.HashMap<>(java.util.Objects.requireNonNull(profilo.getData(), "Profilo non disponibile"));
            dati.put("disponibilitaVersione", versione == null ? 1L : versione + 1);
            transaction.set(db.collection("professori").document(profId), DatiPubblici.professore(dati));
            transaction.delete(db.collection("assenze").document(id));
            transaction.delete(ref);
            transaction.update(prof, "disponibilitaVersione", versione == null ? 1L : versione + 1);
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}
