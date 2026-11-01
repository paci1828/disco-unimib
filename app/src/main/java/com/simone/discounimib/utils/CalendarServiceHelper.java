package com.simone.discounimib.utils;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.simone.discounimib.domain.DatiEventoCalendario;
import com.simone.discounimib.domain.ProtocolloSincronizzazione;
import com.simone.discounimib.models.Appuntamento;
import java.io.IOException;

public final class CalendarServiceHelper {
    private final Calendar service;

    public CalendarServiceHelper(String accessToken) {
        service = new Calendar.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            request.setConnectTimeout(15000);
            request.setReadTimeout(15000);
            request.setNumberOfRetries(0);
        }).setApplicationName("DISCo Calendar").build();
    }

    public String sincronizza(Appuntamento app) throws IOException {
        String id = app.getEventoGoogleCalendarId();
        if (id == null || id.isEmpty()) id = ProtocolloSincronizzazione.idEvento(app.getId());
        if (!"accettato".equals(app.getStato())) {
            try {
                service.events().delete("primary", id).setSendUpdates("none").execute();
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != 404 && e.getStatusCode() != 410) throw e;
            }
            return id;
        }
        DatiEventoCalendario tempi = new DatiEventoCalendario(app.getData(), app.getOraInizio(), app.getOraFine());
        Event evento = new Event().setSummary("Ricevimento DISCo")
                .setDescription("Con studente: " + app.getNomeStudente())
                .setStart(new EventDateTime().setDateTime(new DateTime(tempi.inizio)).setTimeZone("Europe/Rome"))
                .setEnd(new EventDateTime().setDateTime(new DateTime(tempi.fine)).setTimeZone("Europe/Rome"));
        try {
            service.events().get("primary", id).execute();
            service.events().patch("primary", id, evento).setSendUpdates("none").execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 404) throw e;
            try {
                service.events().insert("primary", evento.setId(id)).setSendUpdates("none").execute();
            } catch (HttpResponseException conflitto) {
                if (conflitto.getStatusCode() != 409) throw conflitto;
                service.events().patch("primary", id, evento.setId(null)).setSendUpdates("none").execute();
            }
        }
        return id;
    }
}
