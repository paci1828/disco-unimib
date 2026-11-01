package com.simone.discounimib.repositories;

import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import java.util.HashMap;
import java.util.Map;

/** Proiezioni consultabili: nessuna email, matricola o motivazione. */
final class DatiPubblici {
    private DatiPubblici() {}

    static Map<String, Object> professore(Map<String, Object> dati) {
        Map<String, Object> pubblico = new HashMap<>();
        for (String campo : new String[]{"uid", "nome", "cognome", "ruolo", "giornoRicevimento",
                "oraInizio", "oraFine", "durataAppuntamento", "disponibilitaVersione"}) {
            pubblico.put(campo, dati.get(campo));
        }
        return pubblico;
    }

    static Map<String, Object> occupazione(Appuntamento app) {
        Map<String, Object> dati = new HashMap<>();
        dati.put("idProfessore", app.getIdProfessore());
        dati.put("data", app.getData());
        dati.put("oraInizio", app.getOraInizio());
        dati.put("oraFine", app.getOraFine());
        dati.put("stato", app.getStato());
        return dati;
    }

    static Map<String, Object> assenza(Indisponibilita ind) {
        Map<String, Object> dati = new HashMap<>();
        dati.put("idProfessore", ind.getIdProfessore());
        dati.put("dataInizio", ind.getDataInizio());
        dati.put("dataFine", ind.getDataFine());
        dati.put("oraInizio", ind.getOraInizio());
        dati.put("oraFine", ind.getOraFine());
        return dati;
    }

    static String chiaveSlot(Appuntamento app) {
        com.google.firebase.Timestamp value = app.getInizioIstante() == null
                ? istante(app.getData(), app.getOraInizio()) : app.getInizioIstante();
        return app.getIdProfessore() + "_" + value.toDate().getTime();
    }

    static com.google.firebase.Timestamp istante(String data, String ora) {
        java.time.LocalDateTime value = java.time.LocalDate.parse(data,
                com.simone.discounimib.domain.CalcolatoreDisponibilita.DATA).atTime(
                java.time.LocalTime.parse(ora, com.simone.discounimib.domain.CalcolatoreDisponibilita.ORA));
        java.time.ZoneId zone = com.simone.discounimib.domain.CalcolatoreDisponibilita.FUSO;
        if (zone.getRules().getValidOffsets(value).size() != 1) {
            throw new IllegalArgumentException("Orario inesistente o ambiguo");
        }
        java.time.Instant instant = value.atZone(zone).toInstant();
        return new com.google.firebase.Timestamp(instant.getEpochSecond(), instant.getNano());
    }

    static Map<String, Object> blocco(Appuntamento app) {
        Map<String, Object> dati = new HashMap<>();
        dati.put("appuntamentoId", app.getId());
        dati.put("idProfessore", app.getIdProfessore());
        dati.put("data", app.getData());
        dati.put("oraInizio", app.getOraInizio());
        return dati;
    }
}
