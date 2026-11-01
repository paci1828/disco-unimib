package com.simone.discounimib.repositories;

import com.simone.discounimib.domain.CalcolatoreDisponibilita;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.models.Indisponibilita;
import com.simone.discounimib.models.Professore;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class DisponibilitaMapperTest {
    private final LocalDate data = LocalDate.of(2099, 11, 2);
    private final String testoData = data.format(CalcolatoreDisponibilita.DATA);

    private Professore docente() {
        return new Professore("p", "p@example.com", "Nome", "Cognome", "professore", null,
                data.getDayOfWeek().name(), "10:00", "11:00", 30);
    }

    private Appuntamento appuntamento(String stato) {
        Appuntamento app = new Appuntamento();
        app.setData(testoData);
        app.setOraInizio("10:00");
        app.setOraFine("10:30");
        app.setStato(stato);
        return app;
    }

    @Test public void sospesiEAccettatiOccupanoLoSlot() {
        for (String stato : Arrays.asList("sospeso", "accettato")) {
            assertEquals(Collections.singletonList("10:30"), DisponibilitaMapper.slots(docente(),
                    testoData, Collections.emptyList(), Collections.singletonList(appuntamento(stato))));
        }
    }

    @Test public void rifiutatiEAnnullatiLiberanoLoSlot() {
        for (String stato : Arrays.asList("rifiutato", "annullato")) {
            assertEquals(Arrays.asList("10:00", "10:30"), DisponibilitaMapper.slots(docente(),
                    testoData, Collections.emptyList(), Collections.singletonList(appuntamento(stato))));
        }
    }

    @Test public void statoSconosciutoNonLiberaLoSlot() {
        assertEquals(Collections.singletonList("10:30"), DisponibilitaMapper.slots(docente(),
                testoData, Collections.emptyList(), Collections.singletonList(appuntamento(null))));
    }

    @Test public void altreDateNonOccupanoLoSlot() {
        Appuntamento app = appuntamento("accettato");
        app.setData(data.minusDays(1).format(CalcolatoreDisponibilita.DATA));
        assertEquals(2, DisponibilitaMapper.slots(docente(), testoData, Collections.emptyList(),
                Collections.singletonList(app)).size());
    }

    private Indisponibilita eccezione() {
        Indisponibilita ind = new Indisponibilita();
        ind.setDataInizio(data.minusDays(1).format(CalcolatoreDisponibilita.DATA));
        ind.setDataFine(data.plusDays(1).format(CalcolatoreDisponibilita.DATA));
        return ind;
    }

    @Test public void orariNullOVuotiSonoGiornateIntere() {
        Indisponibilita ind = eccezione();
        assertTrue(DisponibilitaMapper.slots(docente(), testoData, Collections.singletonList(ind),
                Collections.emptyList()).isEmpty());
        ind.setOraInizio("");
        ind.setOraFine("");
        assertTrue(DisponibilitaMapper.slots(docente(), testoData, Collections.singletonList(ind),
                Collections.emptyList()).isEmpty());
    }

    @Test public void intervalloPiuGiorniComprendeDateIntermedie() {
        Indisponibilita ind = eccezione();
        ind.setOraInizio("12:00");
        ind.setOraFine("09:00");
        assertTrue(DisponibilitaMapper.slots(docente(), testoData, Collections.singletonList(ind),
                Collections.emptyList()).isEmpty());
    }

    @Test(expected = java.time.format.DateTimeParseException.class)
    public void dataMalformataNonProduceSlotLiberi() {
        Indisponibilita ind = eccezione();
        ind.setDataInizio("31/02/2099");
        DisponibilitaMapper.slots(docente(), testoData, Collections.singletonList(ind), Collections.emptyList());
    }
}
