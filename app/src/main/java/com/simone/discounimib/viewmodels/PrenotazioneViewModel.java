package com.simone.discounimib.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.repositories.AppuntamentiRepository;
import com.simone.discounimib.repositories.AppuntamentiRepository.Disponibilita;
import com.simone.discounimib.utils.StatoRichiesta;

public class PrenotazioneViewModel extends ViewModel {
    private final AppuntamentiRepository repository = new AppuntamentiRepository();
    private final SavedStateHandle salvato;
    private final MutableLiveData<StatoRichiesta<Disponibilita>> disponibilita = new MutableLiveData<>();
    private final MutableLiveData<StatoRichiesta<Boolean>> prenotazione = new MutableLiveData<>();
    private String profId;
    private String data;

    public PrenotazioneViewModel(SavedStateHandle salvato) { this.salvato = salvato; }

    public LiveData<StatoRichiesta<Disponibilita>> getDisponibilita() { return disponibilita; }
    public LiveData<StatoRichiesta<Boolean>> getPrenotazione() { return prenotazione; }
    public String getOrario() { return salvato.get("orario"); }
    public void setOrario(String orario) { salvato.set("orario", orario); }
    public String getMotivazione() {
        String valore = salvato.get("motivazione");
        return valore == null ? "" : valore;
    }
    public void setMotivazione(String testo) { salvato.set("motivazione", testo); }

    public void inizializza(String profId, String data) {
        this.profId = profId;
        this.data = data;
        if (disponibilita.getValue() == null) ricarica();
    }

    public void ricarica() {
        disponibilita.setValue(StatoRichiesta.caricamento());
        repository.caricaDisponibilita(profId, data, new AppuntamentiRepository.OnDisponibilita() {
            @Override public void onSuccess(Disponibilita risultato) {
                if (!risultato.slots.contains(getOrario())) setOrario(null);
                disponibilita.setValue(StatoRichiesta.successo(risultato));
            }
            @Override public void onError(String message) {
                setOrario(null);
                disponibilita.setValue(StatoRichiesta.errore(message));
            }
        });
    }

    public void prenota(String nomeStudente) {
        StatoRichiesta<Boolean> operazione = prenotazione.getValue();
        if (operazione != null && operazione.caricamento) return;
        StatoRichiesta<Disponibilita> stato = disponibilita.getValue();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || stato == null || stato.dati == null || getOrario() == null
                || !stato.dati.slots.contains(getOrario()) || getMotivazione().trim().isEmpty()) {
            prenotazione.setValue(StatoRichiesta.errore("Seleziona un orario e inserisci la motivazione"));
            return;
        }
        Appuntamento app = new Appuntamento();
        app.setIdProfessore(profId);
        app.setIdStudente(user.getUid());
        app.setNomeProfessore(stato.dati.professore.getNome() + " " + stato.dati.professore.getCognome());
        app.setEmailProfessore(stato.dati.professore.getEmail());
        app.setNomeStudente(nomeStudente);
        app.setEmailStudente(user.getEmail());
        app.setData(data);
        app.setOraInizio(getOrario());
        app.setMotivazione(getMotivazione().trim());
        app.setDataCreazione(Timestamp.now());
        prenotazione.setValue(StatoRichiesta.caricamento());
        repository.creaAppuntamento(app, new AppuntamentiRepository.OnTaskCompleted() {
            @Override public void onSuccess() { prenotazione.setValue(StatoRichiesta.successo(true)); }
            @Override public void onError(String message) {
                prenotazione.setValue(StatoRichiesta.errore(message));
                ricarica();
            }
        });
    }

    public void consumaEsito() { prenotazione.setValue(null); }
}
