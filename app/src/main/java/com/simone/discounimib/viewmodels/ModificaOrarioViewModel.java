package com.simone.discounimib.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Professore;
import com.simone.discounimib.repositories.OrarioDocenteDataSource;
import com.simone.discounimib.repositories.ProfessoriRepository;
import com.simone.discounimib.utils.StatoRichiesta;
import java.time.LocalTime;
import java.util.Objects;

public class ModificaOrarioViewModel extends ViewModel {
    private final SavedStateHandle form;
    private final OrarioDocenteDataSource repository;
    private final MutableLiveData<StatoRichiesta<Professore>> caricamento = new MutableLiveData<>();
    private final MutableLiveData<StatoRichiesta<Boolean>> salvataggio = new MutableLiveData<>();
    private String uid;
    private int richiesta;

    public ModificaOrarioViewModel(SavedStateHandle form) { this(form, new ProfessoriRepository()); }
    ModificaOrarioViewModel(SavedStateHandle form, OrarioDocenteDataSource repository) {
        this.form = form;
        this.repository = repository;
    }
    public LiveData<StatoRichiesta<Professore>> getCaricamento() { return caricamento; }
    public LiveData<StatoRichiesta<Boolean>> getSalvataggio() { return salvataggio; }
    public String getGiorno() { return testo("giorno"); }
    public String getInizio() { return testo("inizio"); }
    public String getFine() { return testo("fine"); }
    public String getDurata() { return testo("durata"); }
    private String testo(String key) { String s = form.get(key); return s == null ? "" : s; }
    private void modifica(String key, String valore) {
        if (Objects.equals(testo(key), valore)) return;
        form.set(key, valore);
        form.set("modificato", true);
        if (!occupato()) salvataggio.setValue(null);
    }
    public void setGiorno(String s) { modifica("giorno", s); }
    public void setInizio(String s) { modifica("inizio", s); }
    public void setFine(String s) { modifica("fine", s); }
    public void setDurata(String s) { modifica("durata", s); }
    public boolean occupato() {
        return (caricamento.getValue() != null && caricamento.getValue().caricamento)
                || (salvataggio.getValue() != null && salvataggio.getValue().caricamento);
    }
    public boolean daRicaricare() {
        return caricamento.getValue() != null && caricamento.getValue().errore != null;
    }
    public void inizializza(String uid) {
        if (caricamento.getValue() != null && Objects.equals(this.uid, uid)) return;
        String precedenteUid = form.get("uid");
        if (precedenteUid != null && !Objects.equals(precedenteUid, uid)) {
            for (String key : new String[]{"giorno", "inizio", "fine", "durata"}) form.set(key, "");
            form.set("modificato", false);
        }
        if (!Objects.equals(this.uid, uid)) {
            richiesta++;
            caricamento.setValue(null);
            salvataggio.setValue(null);
        }
        this.uid = uid;
        form.set("uid", uid);
        ricarica();
    }
    public void ricarica() {
        if (occupato()) return;
        int numero = ++richiesta;
        salvataggio.setValue(null);
        caricamento.setValue(StatoRichiesta.caricamento());
        repository.caricaOrario(uid, new OrarioDocenteDataSource.Caricamento() {
            @Override public void onSuccess(Professore prof) {
                if (numero != richiesta) return;
                if (!Boolean.TRUE.equals(form.get("modificato"))) {
                    form.set("giorno", prof.getGiornoRicevimento());
                    form.set("inizio", prof.getOraInizio());
                    form.set("fine", prof.getOraFine());
                    form.set("durata", String.valueOf(prof.getDurataAppuntamento()));
                }
                caricamento.setValue(StatoRichiesta.successo(prof));
            }
            @Override public void onError(String message) {
                if (numero == richiesta) caricamento.setValue(StatoRichiesta.errore(message));
            }
        });
    }
    public void salva() {
        if (occupato()) return;
        if (caricamento.getValue() == null || caricamento.getValue().dati == null) {
            ricarica();
            return;
        }
        final int durata;
        try {
            if (getGiorno().trim().isEmpty() || getInizio().trim().isEmpty()
                    || getFine().trim().isEmpty() || getDurata().trim().isEmpty()) {
                throw new IllegalArgumentException("Compila tutti i campi");
            }
            durata = Integer.parseInt(getDurata().trim());
            CalcolatoreDisponibilita.giorno(getGiorno());
            CalcolatoreDisponibilita.validaOrario(
                    LocalTime.parse(getInizio().trim(), CalcolatoreDisponibilita.ORA),
                    LocalTime.parse(getFine().trim(), CalcolatoreDisponibilita.ORA), durata);
        } catch (RuntimeException e) {
            salvataggio.setValue(StatoRichiesta.errore("Controlla giorno, orari e durata: la durata deve essere positiva e rientrare nella fascia."));
            return;
        }
        int numero = richiesta;
        salvataggio.setValue(StatoRichiesta.caricamento());
        repository.salvaOrario(uid, getGiorno().trim(), getInizio().trim(), getFine().trim(), durata,
                new OrarioDocenteDataSource.Esito() {
                    @Override public void onSuccess() {
                        if (numero != richiesta) return;
                        form.set("modificato", false);
                        salvataggio.setValue(StatoRichiesta.successo(true));
                    }
                    @Override public void onError(String message) {
                        if (numero == richiesta) salvataggio.setValue(StatoRichiesta.errore(message));
                    }
                });
    }
}
