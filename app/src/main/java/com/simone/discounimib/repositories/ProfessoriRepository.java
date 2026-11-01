package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.utils.StatoRichiesta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfessoriRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Professore>> getAllProfessori() {
        return new MutableLiveData<List<Professore>>() {
            private ListenerRegistration listener;
            @Override protected void onActive() {
                listener = db.collection("users").whereEqualTo("ruolo", "professore")
                        .addSnapshotListener((value, error) -> setValue(
                                error == null && value != null ? value.toObjects(Professore.class) : null));
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
            }
        };
    }

    public LiveData<Professore> getProfessoreData(String uid) {
        MutableLiveData<Professore> risultato = new MutableLiveData<>();
        if (uid == null || uid.isEmpty()) {
            risultato.setValue(null);
            return risultato;
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> risultato.setValue(doc.toObject(Professore.class)))
                .addOnFailureListener(e -> risultato.setValue(null));
        return risultato;
    }

    public LiveData<StatoRichiesta<List<Professore>>> getProfessoriDisponibili(String data) {
        MutableLiveData<StatoRichiesta<List<Professore>>> stato = new MutableLiveData<>();
        stato.setValue(StatoRichiesta.caricamento());
        db.collection("users").whereEqualTo("ruolo", "professore").get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    List<Professore> candidati = new ArrayList<>();
                    List<Task<QuerySnapshot>> richieste = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Professore prof = doc.toObject(Professore.class);
                        if (prof == null) continue;
                        prof.setUid(doc.getId());
                        try {
                            if (CalcolatoreDisponibilita.calcola(LocalDate.parse(data, CalcolatoreDisponibilita.DATA),
                                    CalcolatoreDisponibilita.giorno(prof.getGiornoRicevimento()),
                                    LocalTime.parse(prof.getOraInizio(), CalcolatoreDisponibilita.ORA),
                                    LocalTime.parse(prof.getOraFine(), CalcolatoreDisponibilita.ORA),
                                    prof.getDurataAppuntamento(), Collections.emptyList(),
                                    LocalDateTime.now(CalcolatoreDisponibilita.FUSO)).isEmpty()) continue;
                        } catch (RuntimeException e) {
                            continue; // Un docente senza un orario valido non è prenotabile.
                        }
                        candidati.add(prof);
                        richieste.add(db.collection("indisponibilita").whereEqualTo("idProfessore", prof.getUid())
                                .get(Source.SERVER));
                    }
                    Tasks.whenAllSuccess(richieste).addOnSuccessListener(ignored -> {
                        List<Professore> disponibili = new ArrayList<>();
                        try {
                            for (int i = 0; i < candidati.size(); i++) {
                                List<Indisponibilita> eccezioni = richieste.get(i).getResult()
                                        .toObjects(Indisponibilita.class);
                                if (!DisponibilitaMapper.slots(candidati.get(i), data, eccezioni,
                                        Collections.emptyList()).isEmpty()) disponibili.add(candidati.get(i));
                            }
                            stato.setValue(StatoRichiesta.successo(disponibili));
                        } catch (RuntimeException e) {
                            stato.setValue(StatoRichiesta.errore("Impossibile verificare le indisponibilità"));
                        }
                    }).addOnFailureListener(e -> stato.setValue(StatoRichiesta.errore(
                            "Impossibile caricare le disponibilità. Tocca per riprovare.")));
                }).addOnFailureListener(e -> stato.setValue(StatoRichiesta.errore(
                        "Impossibile caricare i docenti. Tocca per riprovare.")));
        return stato;
    }

    public void salvaOrario(String uid, String giorno, String inizio, String fine, int durata,
                            OnTaskCompleted callback) {
        try {
            CalcolatoreDisponibilita.giorno(giorno);
            CalcolatoreDisponibilita.validaOrario(LocalTime.parse(inizio, CalcolatoreDisponibilita.ORA),
                    LocalTime.parse(fine, CalcolatoreDisponibilita.ORA), durata);
        } catch (RuntimeException e) {
            callback.onError("Giorno, fascia oraria o durata non validi");
            return;
        }
        if (uid == null || uid.isEmpty()) {
            callback.onError("Sessione non valida");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("giornoRicevimento", giorno);
        updates.put("oraInizio", inizio);
        updates.put("oraFine", fine);
        updates.put("durataAppuntamento", durata);
        DocumentReference ref = db.collection("users").document(uid);
        db.runTransaction(transaction -> {
            Long versione = transaction.get(ref).getLong("disponibilitaVersione");
            updates.put("disponibilitaVersione", versione == null ? 1L : versione + 1);
            transaction.update(ref, updates);
            return null;
        }).addOnSuccessListener(ignored -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}
