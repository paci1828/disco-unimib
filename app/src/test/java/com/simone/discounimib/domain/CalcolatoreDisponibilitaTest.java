package com.simone.discounimib.domain;

import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class CalcolatoreDisponibilitaTest {
    private final LocalDate lunedi = LocalDate.of(2026, 11, 2);
    private final LocalDateTime prima = lunedi.minusDays(1).atStartOfDay();

    private List<String> slots(int durata, CalcolatoreDisponibilita.Intervallo... occupati) {
        return CalcolatoreDisponibilita.calcola(lunedi, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 10), durata, Arrays.asList(occupati), prima);
    }

    @Test public void escludeSlotCheTerminanoOltreLaFascia() {
        assertEquals(Arrays.asList("10:00", "10:30"), slots(30));
    }

    @Test public void escludeAncheSovrapposizioniParziali() {
        assertEquals(Collections.singletonList("10:30"), slots(30,
                new CalcolatoreDisponibilita.Intervallo(lunedi.atTime(10, 15), lunedi.atTime(10, 30))));
    }

    @Test public void estremiAdiacentiNonSiSovrappongono() {
        assertEquals(Collections.singletonList("10:00"), slots(30,
                new CalcolatoreDisponibilita.Intervallo(lunedi.atTime(10, 30), lunedi.atTime(11, 0))));
    }

    @Test public void indisponibilitaPiuGiorniBloccaAncheGiorniIntermedi() {
        assertTrue(slots(30, new CalcolatoreDisponibilita.Intervallo(
                lunedi.minusDays(1).atStartOfDay(), lunedi.plusDays(2).atStartOfDay())).isEmpty());
    }

    @Test public void escludeGiornoSettimanaleDiverso() {
        assertTrue(CalcolatoreDisponibilita.calcola(lunedi, DayOfWeek.TUESDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 30, Collections.emptyList(), prima).isEmpty());
    }

    @Test public void escludeOrariPassatiEDiInizioGiaRaggiunto() {
        assertEquals(Collections.singletonList("10:30"), CalcolatoreDisponibilita.calcola(
                lunedi, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0),
                30, Collections.emptyList(), lunedi.atTime(10, 0)));
    }

    @Test public void escludeDatePassate() {
        assertTrue(CalcolatoreDisponibilita.calcola(lunedi, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 30,
                Collections.emptyList(), lunedi.plusDays(1).atStartOfDay()).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class) public void rifiutaDurataZero() { slots(0); }
    @Test(expected = IllegalArgumentException.class) public void rifiutaDurataNegativa() { slots(-1); }
    @Test(expected = IllegalArgumentException.class) public void rifiutaDurataEccessiva() { slots(90); }
    @Test(expected = java.time.format.DateTimeParseException.class)
    public void rifiutaDataInesistente() { LocalDate.parse("31/02/2026", CalcolatoreDisponibilita.DATA); }
    @Test(expected = java.time.format.DateTimeParseException.class)
    public void rifiutaOrarioInesistente() { LocalTime.parse("25:00", CalcolatoreDisponibilita.ORA); }

    @Test public void riconosceGiorniItalianiConOSenzaAccento() {
        assertEquals(DayOfWeek.MONDAY, CalcolatoreDisponibilita.giorno(" LUNEDÌ "));
        assertEquals(DayOfWeek.FRIDAY, CalcolatoreDisponibilita.giorno("venerdi"));
    }
}
