package com.simone.discounimib.domain;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class RegoleAppuntamentoTest {
    @Test public void docenteAccettaORifiutaSoloRichiesteSospese() {
        RegoleAppuntamento.valida("sospeso", "accettato", "p", "p", "s", true);
        RegoleAppuntamento.valida("sospeso", "rifiutato", "p", "p", "s", true);
    }
    @Test public void entrambiPossonoAnnullareERiprogrammare() {
        for (String uid : new String[]{"p", "s"}) {
            for (String stato : new String[]{"sospeso", "accettato"}) {
                RegoleAppuntamento.valida(stato, "annullato", uid, "p", "s", true);
                RegoleAppuntamento.valida(stato, "riprogramma", uid, "p", "s", true);
            }
        }
    }
    @Test(expected=IllegalArgumentException.class) public void studenteNonPuoAccettare() {
        RegoleAppuntamento.valida("sospeso", "accettato", "s", "p", "s", true);
    }
    @Test(expected=IllegalArgumentException.class) public void estraneoNonPuoAnnullare() {
        RegoleAppuntamento.valida("accettato", "annullato", "altro", "p", "s", true);
    }
    @Test(expected=IllegalArgumentException.class) public void nonSiRiattivaUnAnnullato() {
        RegoleAppuntamento.valida("annullato", "accettato", "p", "p", "s", true);
    }
    @Test(expected=IllegalArgumentException.class) public void nonSiRifiutaUnGiaAccettato() {
        RegoleAppuntamento.valida("accettato", "rifiutato", "p", "p", "s", true);
    }
    @Test(expected=IllegalArgumentException.class) public void passatoNonModificabile() {
        RegoleAppuntamento.valida("accettato", "riprogramma", "p", "p", "s", false);
    }
    @Test(expected=IllegalArgumentException.class) public void esitoSconosciutoNonAmmesso() {
        RegoleAppuntamento.valida("sospeso", "inventato", "p", "p", "s", true);
    }
    @Test public void dataOraDevonoEssereValideEStrettamenteFuture() {
        LocalDateTime ora = LocalDateTime.of(2026,11,2,10,0);
        assertTrue(RegoleAppuntamento.futuro("02/11/2026","10:30",ora));
        assertFalse(RegoleAppuntamento.futuro("02/11/2026","10:00",ora));
        assertFalse(RegoleAppuntamento.futuro("31/02/2026","10:30",ora));
        assertFalse(RegoleAppuntamento.futuro(null,null,ora));
    }
}
