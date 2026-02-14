package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.models.Professore;

import java.util.List;

public class ProfessoriRepository {
    private FirebaseFirestore db;

    public ProfessoriRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Professore>> getAllProfessori() {
        MutableLiveData<List<Professore>> professoriList = new MutableLiveData<>();
        db.collection("users")
                .whereEqualTo("ruolo", "professore")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        professoriList.setValue(null);
                        return;
                    }
                    if (value != null) {
                        professoriList.setValue(value.toObjects(Professore.class));
                    }
                });
        return professoriList;
    }

    public LiveData<Professore> getProfessoreData(String uid) {
        MutableLiveData<Professore> professoreData = new MutableLiveData<>();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        professoreData.setValue(documentSnapshot.toObject(Professore.class));
                    }
                });
        return professoreData;
    }

    public void updateProfessoreSettings(Professore professore, OnTaskCompleted callback) {
        db.collection("users").document(professore.getUid()).set(professore)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}