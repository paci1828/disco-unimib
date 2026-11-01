package com.simone.discounimib.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import com.simone.discounimib.sync.SincronizzazioneScheduler;
import com.simone.discounimib.utils.StatoRichiesta;

public class GestioneAppuntamentoViewModel extends AndroidViewModel {
    private final AppuntamentiRepository repository = new AppuntamentiRepository();
    private final SavedStateHandle salvato;
    private final String id;
    private final LiveData<StatoRichiesta<Appuntamento>> dettagli;
    private final MutableLiveData<StatoRichiesta<AppuntamentiRepository.Disponibilita>> slots = new MutableLiveData<>();
    private final MutableLiveData<StatoRichiesta<String>> operazione = new MutableLiveData<>();
    private int richiesta;

    public GestioneAppuntamentoViewModel(@NonNull Application app, SavedStateHandle salvato) {
        super(app);
        this.salvato = salvato;
        id = salvato.get("appuntamento_id");
        dettagli = repository.getAppuntamento(id);
    }
    public LiveData<StatoRichiesta<Appuntamento>> getDettagli() { return dettagli; }
    public LiveData<StatoRichiesta<AppuntamentiRepository.Disponibilita>> getSlots() { return slots; }
    public LiveData<StatoRichiesta<String>> getOperazione() { return operazione; }
    public String getData() { return salvato.get("nuova_data"); }
    public String getOrario() { return salvato.get("nuovo_orario"); }
    public void setOrario(String ora) { salvato.set("nuovo_orario", ora); }
    public boolean occupato() {
        return operazione.getValue() != null && operazione.getValue().caricamento;
    }
    public void consumaEsito() { operazione.setValue(null); }

    public void cambiaStato(String stato) {
        if (occupato()) return;
        operazione.setValue(StatoRichiesta.caricamento());
        repository.updateAppuntamentoStato(id, stato, callback());
    }
    public void ritentaSync(boolean emailVerificata) {
        if (occupato()) return;
        operazione.setValue(StatoRichiesta.caricamento());
        repository.richiediNuovaSincronizzazione(id, emailVerificata, callback());
    }
    private AppuntamentiRepository.OnTaskCompleted callback() {
        return new AppuntamentiRepository.OnTaskCompleted() {
            @Override public void onSuccess() {
                SincronizzazioneScheduler.richiedi(getApplication());
                operazione.setValue(StatoRichiesta.successo(""));
            }
            @Override public void onError(String message) {
                operazione.setValue(StatoRichiesta.errore(message));
            }
        };
    }
    public void preparaRiprogrammazione(String dataOriginale) {
        if (getData() == null) salvato.set("nuova_data", dataOriginale);
        if (slots.getValue() == null) ricaricaSlots();
    }
    public void scegliData(String data) {
        salvato.set("nuova_data", data);
        setOrario(null);
        ricaricaSlots();
    }
    public void ricaricaSlots() {
        int numero = ++richiesta;
        slots.setValue(StatoRichiesta.caricamento());
        repository.caricaDisponibilitaRiprogrammazione(id, getData(), new AppuntamentiRepository.OnDisponibilita() {
            @Override public void onSuccess(AppuntamentiRepository.Disponibilita disponibilita) {
                if (numero != richiesta) return;
                if (!disponibilita.slots.contains(getOrario())) setOrario(null);
                slots.setValue(StatoRichiesta.successo(disponibilita));
            }
            @Override public void onError(String message) {
                if (numero != richiesta) return;
                setOrario(null);
                slots.setValue(StatoRichiesta.errore(message));
            }
        });
    }
    public void riprogramma() {
        if (occupato()) return;
        if (getData() == null || getOrario() == null) {
            operazione.setValue(StatoRichiesta.errore("Seleziona una data e un orario"));
            return;
        }
        operazione.setValue(StatoRichiesta.caricamento());
        repository.riprogramma(id, getData(), getOrario(), new AppuntamentiRepository.OnRiprogramma() {
            @Override public void onSuccess(String nuovoId) {
                SincronizzazioneScheduler.richiedi(getApplication());
                operazione.setValue(StatoRichiesta.successo(nuovoId));
            }
            @Override public void onError(String message) {
                operazione.setValue(StatoRichiesta.errore(message));
                ricaricaSlots();
            }
        });
    }
}
