package com.simone.discounimib.repositories;

import com.simone.discounimib.models.Professore;

/** Accesso al profilo separato dallo stato del form, per gestire anche errori e nuovi tentativi. */
public interface OrarioDocenteDataSource {
    interface Caricamento {
        void onSuccess(Professore professore);
        void onError(String messaggio);
    }
    interface Esito {
        void onSuccess();
        void onError(String messaggio);
    }
    void caricaOrario(String uid, Caricamento callback);
    void salvaOrario(String uid, String giorno, String inizio, String fine, int durata, Esito callback);
}
