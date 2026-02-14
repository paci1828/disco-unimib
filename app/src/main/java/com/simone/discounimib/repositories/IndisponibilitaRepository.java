package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.models.Indisponibilita;

import java.util.List;

public class IndisponibilitaRepository {
    private FirebaseFirestore db;

    public IndisponibilitaRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Indisponibilita>> getIndisponibilitaPerProfessore(String uidProf) {
        MutableLiveData<List<Indisponibilita>> indisponibilitaList = new MutableLiveData<>();
        db.collection("indisponibilita")
                .whereEqualTo("idProfessore", uidProf)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        indisponibilitaList.setValue(null);
                        return;
                    }
                    if (value != null) {
                        indisponibilitaList.setValue(value.toObjects(Indisponibilita.class));
                    }
                });
        return indisponibilitaList;
    }

    public void aggiungiIndisponibilita(Indisponibilita indisponibilita, OnTaskCompleted callback) {
        db.collection("indisponibilita").add(indisponibilita)
                .addOnSuccessListener(documentReference -> {
                    indisponibilita.setId(documentReference.getId());
                    documentReference.update("id", indisponibilita.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void eliminaIndisponibilita(String id, OnTaskCompleted callback) {
        db.collection("indisponibilita").document(id).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}