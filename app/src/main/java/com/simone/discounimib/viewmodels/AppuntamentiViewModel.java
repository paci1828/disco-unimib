package com.simone.discounimib.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import java.util.List;

public class AppuntamentiViewModel extends AndroidViewModel {
    private final AppuntamentiRepository repository;

    public AppuntamentiViewModel(@NonNull Application application) {
        super(application);
        repository = new AppuntamentiRepository();
    }

    public LiveData<List<Appuntamento>> getAppuntamentiProfessore(String uidProf, String stato) {
        return repository.getAppuntamentiPerProfessore(uidProf, stato);
    }

    public LiveData<List<Appuntamento>> getAppuntamentiStudente(String uidStud) {
        return repository.getAppuntamentiPerStudente(uidStud);
    }

    public void updateStato(String id, String nuovoStato, AppuntamentiRepository.OnTaskCompleted callback) {
        repository.updateAppuntamentoStato(id, nuovoStato, callback);
    }
}