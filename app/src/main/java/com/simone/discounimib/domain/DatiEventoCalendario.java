package com.simone.discounimib.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DatiEventoCalendario {
    public final String inizio;
    public final String fine;
    public DatiEventoCalendario(String data, String oraInizio, String oraFine) {
        LocalDate giorno = LocalDate.parse(data, CalcolatoreDisponibilita.DATA);
        OffsetDateTime da = risolvi(giorno.atTime(LocalTime.parse(oraInizio, CalcolatoreDisponibilita.ORA)));
        OffsetDateTime a = risolvi(giorno.atTime(LocalTime.parse(oraFine, CalcolatoreDisponibilita.ORA)));
        if (!a.isAfter(da)) throw new IllegalArgumentException("Durata dell'evento non valida");
        inizio = da.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        fine = a.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    private static OffsetDateTime risolvi(LocalDateTime data) {
        List<ZoneOffset> offsets = CalcolatoreDisponibilita.FUSO.getRules().getValidOffsets(data);
        if (offsets.size() != 1) throw new IllegalArgumentException("Orario ambiguo o inesistente al cambio dell'ora");
        return data.atOffset(offsets.get(0));
    }
}
