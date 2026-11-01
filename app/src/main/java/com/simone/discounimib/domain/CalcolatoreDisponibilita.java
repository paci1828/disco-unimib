package com.simone.discounimib.domain;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Regole di ricevimento indipendenti da Android e Firebase. Intervalli [inizio, fine). */
public final class CalcolatoreDisponibilita {
    public static final ZoneId FUSO = ZoneId.of("Europe/Rome");
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private CalcolatoreDisponibilita() {}

    public static final class Intervallo {
        public final LocalDateTime inizio;
        public final LocalDateTime fine;

        public Intervallo(LocalDateTime inizio, LocalDateTime fine) {
            if (!inizio.isBefore(fine)) throw new IllegalArgumentException("Intervallo non valido");
            this.inizio = inizio;
            this.fine = fine;
        }

        public boolean siSovrappone(LocalDateTime da, LocalDateTime a) {
            return inizio.isBefore(a) && da.isBefore(fine);
        }
    }

    public static DayOfWeek giorno(String testo) {
        if (testo == null) throw new IllegalArgumentException("Seleziona un giorno di ricevimento");
        String valore = Normalizer.normalize(testo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        String[] giorni = {"lunedi", "martedi", "mercoledi", "giovedi", "venerdi", "sabato", "domenica"};
        for (int i = 0; i < giorni.length; i++) {
            if (giorni[i].equals(valore) || DayOfWeek.of(i + 1).name().equalsIgnoreCase(valore)) {
                return DayOfWeek.of(i + 1);
            }
        }
        throw new IllegalArgumentException("Inserisci un giorno della settimana valido");
    }

    public static void validaOrario(LocalTime inizio, LocalTime fine, int durata) {
        if (inizio == null || fine == null || !inizio.isBefore(fine)) {
            throw new IllegalArgumentException("L'ora di inizio deve precedere l'ora di fine");
        }
        long minuti = java.time.Duration.between(inizio, fine).toMinutes();
        if (durata <= 0 || durata > minuti) {
            throw new IllegalArgumentException("La durata deve essere positiva e rientrare nella fascia");
        }
    }

    public static List<String> calcola(LocalDate data, DayOfWeek giorno,
                                      LocalTime inizio, LocalTime fine, int durata,
                                      List<Intervallo> occupati, LocalDateTime adesso) {
        validaOrario(inizio, fine, durata);
        List<String> slots = new ArrayList<>();
        if (data.getDayOfWeek() != giorno || data.isBefore(adesso.toLocalDate())) return slots;
        LocalDateTime limite = data.atTime(fine);
        for (LocalDateTime da = data.atTime(inizio); !da.plusMinutes(durata).isAfter(limite);
             da = da.plusMinutes(durata)) {
            LocalDateTime a = da.plusMinutes(durata);
            if (!da.isAfter(adesso)) continue;
            if (FUSO.getRules().getValidOffsets(da).size() != 1
                    || FUSO.getRules().getValidOffsets(a).size() != 1
                    || !FUSO.getRules().getOffset(da).equals(FUSO.getRules().getOffset(a))) continue;
            boolean libero = true;
            for (Intervallo intervallo : occupati) {
                if (intervallo.siSovrappone(da, a)) {
                    libero = false;
                    break;
                }
            }
            if (libero) slots.add(da.toLocalTime().format(ORA));
        }
        return slots;
    }
}
