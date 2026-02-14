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
        db.collection("users").document(user.getUid()).set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface OnTaskCompleted {
        void onSuccess();
        void onError(String message);
    }
}