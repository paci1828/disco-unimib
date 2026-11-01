package com.simone.discounimib.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class ProtocolloSincronizzazione {
    public enum Email { INVIA, SALTA, DA_VERIFICARE }
    private ProtocolloSincronizzazione() {}
    public static String idEvento(String id) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                    ("discounimib|" + id).getBytes(StandardCharsets.UTF_8));
            StringBuilder valore = new StringBuilder("d");
            for (byte b : bytes) valore.append(String.format(Locale.ROOT, "%02x", b & 255));
            return valore.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static boolean precedenteRiconciliato(String stato, long revisione, long calendarioRevisione) {
        return "annullato".equals(stato) && revisione > 0 && calendarioRevisione >= revisione;
    }
    public static Email email(long versione, long emailVersione, String stato, boolean notifica) {
        if ("in_invio".equals(stato) || "da_verificare".equals(stato)) return Email.DA_VERIFICARE;
        if (!notifica || (versione == emailVersione
                && ("inviata".equals(stato) || "gestita".equals(stato)))) return Email.SALTA;
        return Email.INVIA;
    }
}
