package com.simone.discounimib.utils;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.simone.discounimib.domain.SincronizzaRicevimento;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public final class GmailServiceHelper {
    private final Gmail service;
    private final String mittente;
    public GmailServiceHelper(String accessToken, String mittente) {
        this.mittente = mittente;
        service = new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            request.setConnectTimeout(15000);
            request.setReadTimeout(15000);
            // Nessun retry HTTP implicito dopo una possibile consegna.
            request.setNumberOfRetries(0);
        }).setApplicationName("DISCo Calendar").build();
    }
    public String invia(String destinatario, String oggetto, String testo) throws Exception {
        MimeMessage email = new MimeMessage(Session.getInstance(new Properties()));
        email.setFrom(new InternetAddress(mittente, true));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(destinatario, true));
        email.setSubject(oggetto, "UTF-8");
        email.setText(testo, "UTF-8");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        email.writeTo(output);
        try {
            return service.users().messages().send("me", new Message()
                    .setRaw(Base64.encodeBase64URLSafeString(output.toByteArray()))).execute().getId();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                throw new SincronizzaRicevimento.InvioRifiutato(e);
            }
            throw e;
        }
    }
}
