package com.simone.discounimib.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.simone.discounimib.models.Professore;

import java.util.ArrayList;
import java.util.List;

public class ProfessoriViewModel extends AndroidViewModel {
    private final FirebaseFirestore db;

    public ProfessoriViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<Professore> getDatiProfessore(String uid) {
        MutableLiveData<Professore> professore = new MutableLiveData<>();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                professore.setValue(documentSnapshot.toObject(Professore.class));
            }
        });
        return professore;
    }

    public LiveData<List<Professore>> getProfessoriDisponibili(String data) {
        MutableLiveData<List<Professore>> professoriDisponibili = new MutableLiveData<>();
        
        // 1. Prendo tutti i professori
        db.collection("users").whereEqualTo("ruolo", "professore")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Professore> tuttiIProf = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    tuttiIProf.add(doc.toObject(Professore.class));
                }
                
                // 2. Controllo le indisponibilità per la data scelta
                db.collection("indisponibilita").whereEqualTo("data", data)
                    .get()
                    .addOnSuccessListener(indispSnapshots -> {
                        List<String> idIndisponibili = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : indispSnapshots) {
                            idIndisponibili.add(doc.getString("idProfessore"));
                        }
                        
                        // 3. Filtro la lista
                        List<Professore> filtrati = new ArrayList<>();
                        for (Professore p : tuttiIProf) {
                            if (!idIndisponibili.contains(p.getUid())) {
                                filtrati.add(p);
                            }
                        }
                        professoriDisponibili.setValue(filtrati);
                    });
            });
            
        return professoriDisponibili;
    }

    public LiveData<List<Professore>> getTuttiIProfessori() {
        MutableLiveData<List<Professore>> professori = new MutableLiveData<>();
        db.collection("users").whereEqualTo("ruolo", "professore")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        professori.setValue(value.toObjects(Professore.class));
                    }
                });
        return professori;
    }
}