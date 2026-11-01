package com.simone.discounimib.repositories;

import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.domain.CalcolatoreDisponibilita.Intervallo;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.models.Professore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

final class DisponibilitaMapper {
    private DisponibilitaMapper() {}

    static List<String> slots(Professore prof, String data, List<Indisponibilita> indisponibilita,
                              List<Appuntamento> appuntamenti) {
        LocalDate giorno = LocalDate.parse(data, CalcolatoreDisponibilita.DATA);
        List<Intervallo> occupati = new ArrayList<>();
        for (Indisponibilita ind : indisponibilita) {
            LocalDate da = LocalDate.parse(ind.getDataInizio(), CalcolatoreDisponibilita.DATA);
            LocalDate a = vuoto(ind.getDataFine()) ? da
                    : LocalDate.parse(ind.getDataFine(), CalcolatoreDisponibilita.DATA);
            if (a.isBefore(da)) throw new IllegalArgumentException("Indisponibilità con date non valide");
            if (giorno.isBefore(da) || giorno.isAfter(a)) continue;
            if (vuoto(ind.getOraInizio()) && vuoto(ind.getOraFine())) {
                occupati.add(new Intervallo(da.atStartOfDay(), a.plusDays(1).atStartOfDay()));
            } else {
                occupati.add(new Intervallo(da.atTime(LocalTime.parse(ind.getOraInizio(), CalcolatoreDisponibilita.ORA)),
                        a.atTime(LocalTime.parse(ind.getOraFine(), CalcolatoreDisponibilita.ORA))));
            }
        }
        for (Appuntamento app : appuntamenti) {
            if (!data.equals(app.getData()) || !occupaSlot(app.getStato())) continue;
            occupati.add(new Intervallo(giorno.atTime(LocalTime.parse(app.getOraInizio(), CalcolatoreDisponibilita.ORA)),
                    giorno.atTime(LocalTime.parse(app.getOraFine(), CalcolatoreDisponibilita.ORA))));
        }
        return CalcolatoreDisponibilita.calcola(giorno,
                CalcolatoreDisponibilita.giorno(prof.getGiornoRicevimento()),
                LocalTime.parse(prof.getOraInizio(), CalcolatoreDisponibilita.ORA),
                LocalTime.parse(prof.getOraFine(), CalcolatoreDisponibilita.ORA),
                prof.getDurataAppuntamento(), occupati,
                LocalDateTime.now(CalcolatoreDisponibilita.FUSO));
    }

    static boolean occupaSlot(String stato) {
        // Uno stato sconosciuto non deve liberare accidentalmente un appuntamento.
        return !"rifiutato".equalsIgnoreCase(stato) && !"annullato".equalsIgnoreCase(stato);
    }

    private static boolean vuoto(String valore) {
        return valore == null || valore.trim().isEmpty();
    }
}
