package com.simone.discounimib.domain;

/** Pipeline testabile: il checkpoint precede l'invio e sopravvive alla perdita della risposta. */
public final class SincronizzaRicevimento {
    public enum Esito { COMPLETATO, SUPERATO, EMAIL_DA_VERIFICARE }
    public interface Archivio {
        boolean attuale() throws Exception;
        void calendarioCompletato(String eventoId) throws Exception;
        boolean prenotaEmail() throws Exception;
        void emailCompletata() throws Exception;
        void emailDaVerificare() throws Exception;
        void emailRifiutata() throws Exception;
        void completa() throws Exception;
    }
    public interface Servizi {
        String calendario() throws Exception;
        void email() throws Exception;
    }
    /** Risposta HTTP 4xx: il servizio ha rifiutato la richiesta, quindi è sicuro ritentare. */
    public static final class InvioRifiutato extends Exception {
        public InvioRifiutato(Throwable cause) { super(cause); }
    }
    public Esito esegui(boolean calendarioAggiornato, ProtocolloSincronizzazione.Email email,
                         Archivio archivio, Servizi servizi) throws Exception {
        if (!archivio.attuale()) return Esito.SUPERATO;
        if (!calendarioAggiornato) archivio.calendarioCompletato(servizi.calendario());
        if (!archivio.attuale()) return Esito.SUPERATO;
        if (email == ProtocolloSincronizzazione.Email.DA_VERIFICARE) {
            archivio.emailDaVerificare();
            return Esito.EMAIL_DA_VERIFICARE;
        }
        if (email == ProtocolloSincronizzazione.Email.INVIA) {
            if (!archivio.prenotaEmail()) return Esito.SUPERATO;
            try {
                servizi.email();
            } catch (InvioRifiutato e) {
                archivio.emailRifiutata();
                throw e;
            } catch (Exception e) {
                archivio.emailDaVerificare();
                return Esito.EMAIL_DA_VERIFICARE;
            }
            // Se questo checkpoint fallisce, "in_invio" impedisce il reinvio al prossimo avvio.
            archivio.emailCompletata();
        }
        archivio.completa();
        return Esito.COMPLETATO;
    }
}
