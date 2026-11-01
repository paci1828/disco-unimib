package com.simone.discounimib.domain;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

public final class RegoleAppuntamento {
    private RegoleAppuntamento() {}
    public static boolean attivo(String stato) {
        return "sospeso".equals(stato) || "accettato".equals(stato);
    }
    public static boolean futuro(String data, String ora, LocalDateTime adesso) {
        try {
            return LocalDate.parse(data, CalcolatoreDisponibilita.DATA)
                    .atTime(LocalTime.parse(ora, CalcolatoreDisponibilita.ORA)).isAfter(adesso);
        } catch (RuntimeException e) { return false; }
    }
    public static void valida(String stato, String esito, String uid, String docente, String studente,
                              boolean futuro) {
        if (uid == null || (!uid.equals(docente) && !uid.equals(studente))) {
            throw new IllegalArgumentException("Appuntamento non autorizzato");
        }
        if (!futuro || !attivo(stato)) throw new IllegalArgumentException("Appuntamento non più modificabile");
        if ("annullato".equals(esito) || "riprogramma".equals(esito)) return;
        if (!uid.equals(docente) || !"sospeso".equals(stato)
                || (!"accettato".equals(esito) && !"rifiutato".equals(esito))) {
            throw new IllegalArgumentException("Transizione non consentita");
        }
    }
}
