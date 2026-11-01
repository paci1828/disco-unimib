package com.simone.discounimib.utils;

/** Stato esplicito di una richiesta, utilizzabile anche da schermate Java/XML. */
public final class StatoRichiesta<T> {
    public final boolean caricamento;
    public final T dati;
    public final String errore;

    private StatoRichiesta(boolean caricamento, T dati, String errore) {
        this.caricamento = caricamento;
        this.dati = dati;
        this.errore = errore;
    }

    public static <T> StatoRichiesta<T> caricamento() {
        return new StatoRichiesta<>(true, null, null);
    }

    public static <T> StatoRichiesta<T> successo(T dati) {
        return new StatoRichiesta<>(false, dati, null);
    }

    public static <T> StatoRichiesta<T> errore(String messaggio) {
        return new StatoRichiesta<>(false, null, messaggio);
    }
}
