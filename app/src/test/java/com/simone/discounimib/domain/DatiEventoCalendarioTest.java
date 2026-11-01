package com.simone.discounimib.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class DatiEventoCalendarioTest {
    @Test public void usaAnnoMeseGiornoEDurataRealeInInverno() {
        DatiEventoCalendario evento = new DatiEventoCalendario("02/11/2026","10:00","10:30");
        assertEquals("2026-11-02T10:00:00+01:00",evento.inizio);
        assertEquals("2026-11-02T10:30:00+01:00",evento.fine);
    }
    @Test public void oraLegaleUsaOffsetDueOre() {
        DatiEventoCalendario evento = new DatiEventoCalendario("02/07/2026","10:00","10:45");
        assertEquals("2026-07-02T10:00:00+02:00",evento.inizio);
        assertEquals("2026-07-02T10:45:00+02:00",evento.fine);
    }
    @Test(expected=IllegalArgumentException.class) public void rifiutaDurataNulla() {
        new DatiEventoCalendario("02/11/2026","10:00","10:00");
    }
    @Test(expected=IllegalArgumentException.class) public void rifiutaFinePrecedente() {
        new DatiEventoCalendario("02/11/2026","10:30","10:00");
    }
    @Test(expected=IllegalArgumentException.class) public void rifiutaOrarioInesistenteAlCambioOra() {
        new DatiEventoCalendario("29/03/2026","02:10","03:10");
    }
    @Test(expected=IllegalArgumentException.class) public void rifiutaOrarioAmbiguoAlCambioOra() {
        new DatiEventoCalendario("25/10/2026","02:10","03:10");
    }
    @Test public void idDeterministicoValidoPerGoogleEUnicoPerAppuntamento() {
        String id = ProtocolloSincronizzazione.idEvento("appuntamento-1");
        assertEquals(id,ProtocolloSincronizzazione.idEvento("appuntamento-1"));
        assertTrue(id.matches("[0-9a-v]{5,1024}"));
        assertNotEquals(id,ProtocolloSincronizzazione.idEvento("appuntamento-2"));
    }
}
