package com.simone.discounimib.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.repositories.ProfessoriRepository;
import com.simone.discounimib.utils.StatoRichiesta;
import java.util.List;

public class ProfessoriViewModel extends ViewModel {
    private final ProfessoriRepository repository = new ProfessoriRepository();

    public LiveData<Professore> getDatiProfessore(String uid) {
        return repository.getProfessoreData(uid);
    }

    public LiveData<StatoRichiesta<List<Professore>>> getProfessoriDisponibili(String data) {
        return repository.getProfessoriDisponibili(data);
    }

    public LiveData<List<Professore>> getTuttiIProfessori() {
        return repository.getAllProfessori();
    }

    public void salvaOrario(String uid, String giorno, String inizio, String fine, int durata,
                            ProfessoriRepository.OnTaskCompleted callback) {
        repository.salvaOrario(uid, giorno, inizio, fine, durata, callback);
    }
}
