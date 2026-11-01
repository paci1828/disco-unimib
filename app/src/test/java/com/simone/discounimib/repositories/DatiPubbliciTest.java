package com.simone.discounimib.repositories;

import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class DatiPubbliciTest {
    @Test public void docenteNonPubblicaEmailMatricolaOTimestamp() {
        Map<String, Object> privato = new HashMap<>();
        privato.put("uid", "prof"); privato.put("email", "privata@example.org");
        privato.put("nome", "Docente"); privato.put("cognome", "Esempio");
        privato.put("ruolo", "professore"); privato.put("matricola", "segreta");
        privato.put("dataCreazione", "riservata"); privato.put("giornoRicevimento", "Lunedì");
        privato.put("oraInizio", "09:00"); privato.put("oraFine", "12:00");
        privato.put("durataAppuntamento", 30); privato.put("disponibilitaVersione", 2L);
        Map<String, Object> pubblico = DatiPubblici.professore(privato);
        assertEquals(9, pubblico.size()); assertEquals(2L, pubblico.get("disponibilitaVersione"));
        assertFalse(pubblico.containsKey("email")); assertFalse(pubblico.containsKey("matricola"));
        assertFalse(pubblico.containsKey("dataCreazione"));
    }
    @Test public void occupazioneNonPubblicaStudenteOMotivo() {
        Appuntamento a = appuntamento(); a.setIdStudente("studente");
        a.setMotivazione("Informazioni riservate"); a.setEmailStudente("privata@example.org");
        Map<String,Object> o = DatiPubblici.occupazione(a);
        assertEquals(5, o.size()); assertEquals("sospeso", o.get("stato"));
        assertFalse(o.containsKey("idStudente")); assertFalse(o.containsKey("motivazione"));
        assertFalse(o.containsKey("emailStudente"));
    }
    @Test public void assenzaNonPubblicaMotivo() {
        Indisponibilita i = new Indisponibilita(); i.setIdProfessore("prof");
        i.setDataInizio("03/11/2026"); i.setDataFine("03/11/2026"); i.setMotivo("Privato");
        Map<String,Object> a = DatiPubblici.assenza(i);
        assertEquals(5, a.size()); assertFalse(a.containsKey("motivo"));
        assertNull(a.get("oraInizio"));
    }
    @Test public void bloccoDipendeDalloSlotENonDallIdentitaDelloStudente() {
        Appuntamento a = appuntamento();
        assertEquals("prof_1793692800000", DatiPubblici.chiaveSlot(a));
        String chiave = DatiPubblici.chiaveSlot(a);
        a.setId("altro"); a.setIdStudente("altro-studente");
        assertEquals(chiave, DatiPubblici.chiaveSlot(a));
        assertEquals("altro", DatiPubblici.blocco(a).get("appuntamentoId"));
    }
    private Appuntamento appuntamento() {
        Appuntamento a = new Appuntamento(); a.setId("id"); a.setIdProfessore("prof");
        a.setData("03/11/2026"); a.setOraInizio("09:00"); a.setOraFine("09:30"); a.setStato("sospeso");
        return a;
    }
}
