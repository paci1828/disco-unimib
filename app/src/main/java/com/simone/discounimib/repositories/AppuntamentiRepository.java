package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.simone.discounimib.models.Appuntamento;

import java.util.List;

public class AppuntamentiRepository {
    private FirebaseFirestore db;

    public AppuntamentiRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Appuntamento>> getAppuntamentiPerProfessore(String uidProf, String stato) {
        MutableLiveData<List<Appuntamento>> appuntamenti = new MutableLiveData<>();
        Query query = db.collection("appuntamenti")
                .whereEqualTo("idProfessore", uidProf);
        
        if (stato != null) {
            query = query.whereEqualTo("stato", stato);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                appuntamenti.setValue(null);
                return;
            }
            if (value != null) {
                appuntamenti.setValue(value.toObjects(Appuntamento.class));
            }
        });
        return appuntamenti;
    }

    public LiveData<List<Appuntamento>> getAppuntamentiPerStudente(String uidStud) {
        MutableLiveData<List<Appuntamento>> appuntamenti = new MutableLiveData<>();
        db.collection("appuntamenti")
                .whereEqualTo("idStudente", uidStud)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        appuntamenti.setValue(null);
                        return;
                    }
                    if (value != null) {
                        appuntamenti.setValue(value.toObjects(Appuntamento.class));
                    }
                });
        return appuntamenti;
    }

    public void updateAppuntamentoStato(String id, String nuovoStato, OnTaskCompleted callback) {
        db.collection("appuntamenti").document(id)
                .update("stato", nuovoStato)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void creaAppuntamento(Appuntamento appuntamento, OnTaskCompleted callback) {
        db.collection("appuntamenti").add(appuntamento)
                .addOnSuccessListener(documentReference -> {
                    appuntamento.setId(documentReference.getId());
                    documentReference.update("id", appuntamento.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}