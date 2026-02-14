package com.simone.discounimib.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.repositories.IndisponibilitaRepository;
import java.util.List;

public class IndisponibilitaViewModel extends AndroidViewModel {
    private IndisponibilitaRepository repository;

    public IndisponibilitaViewModel(@NonNull Application application) {
        super(application);
        repository = new IndisponibilitaRepository();
    }

    public LiveData<List<Indisponibilita>> getIndisponibilitaProfessore(String uidProf) {
        return repository.getIndisponibilitaPerProfessore(uidProf);
    }

    public void aggiungiIndisponibilita(Indisponibilita indisponibilita, IndisponibilitaRepository.OnTaskCompleted callback) {
        repository.aggiungiIndisponibilita(indisponibilita, callback);
    }

    public void eliminaIndisponibilita(String id, IndisponibilitaRepository.OnTaskCompleted callback) {
        repository.eliminaIndisponibilita(id, callback);
    }
}