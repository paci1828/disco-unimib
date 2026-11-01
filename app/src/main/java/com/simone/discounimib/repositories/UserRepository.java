package com.simone.discounimib.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.simone.discounimib.models.User;

public class UserRepository {
    private FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<User> getUser(String uid) {
        MutableLiveData<User> userData = new MutableLiveData<>();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userData.setValue(documentSnapshot.toObject(User.class));
                    } else {
                        userData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> userData.setValue(null));
        return userData;
    }

    public void updateUser(User user, OnTaskCompleted callback) {
        com.google.firebase.firestore.DocumentReference ref = db.collection("users").document(user.getUid());
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot doc = transaction.get(ref);
            java.util.Map<String, Object> dati = new java.util.HashMap<>(java.util.Objects.requireNonNull(doc.getData(), "Profilo non disponibile"));
            java.util.Map<String, Object> modifiche = new java.util.HashMap<>();
            modifiche.put("nome", user.getNome());
            modifiche.put("cognome", user.getCognome());
            modifiche.put("matricola", user.getMatricola());
            dati.putAll(modifiche);
            transaction.update(ref, modifiche);
            if ("professore".equals(doc.getString("ruolo"))) {
                transaction.set(db.collection("professori").document(user.getUid()), DatiPubblici.professore(dati));
            }
            return null;
        })
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}