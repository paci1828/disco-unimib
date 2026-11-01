package com.simone.discounimib.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.HttpResponseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.simone.discounimib.domain.ProtocolloSincronizzazione;
import com.simone.discounimib.domain.SincronizzaRicevimento;
import com.simone.discounimib.models.Appuntamento;
import com.simone.discounimib.utils.CalendarServiceHelper;
import com.simone.discounimib.utils.GmailServiceHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Outbox persistente nel documento appuntamento, con lease per più dispositivi del docente. */
public class SincronizzaAppuntamentiWorker extends Worker {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;
    private long avvio;

    public SincronizzaAppuntamentiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        uid = getInputData().getString("uid");
        avvio = System.currentTimeMillis();
        if (!sessione() || !GoogleSyncAuth.abilitata(getApplicationContext(), uid)) return Result.success();
        boolean ritenta = false;
        try {
            if (!"professore".equals(attendi(db.collection("users").document(uid)
                    .get(Source.SERVER)).getString("ruolo"))) return Result.failure();
            java.util.List<DocumentSnapshot> documenti = new java.util.ArrayList<>(attendi(db.collection("appuntamenti")
                    .whereEqualTo("idProfessore", uid).get(Source.SERVER)).getDocuments());
            documenti.sort(java.util.Comparator.comparingInt(d -> "annullato".equals(d.getString("stato")) ? 0 : 1));
            for (DocumentSnapshot doc : documenti) {
                if (!sessione() || System.currentTimeMillis() - avvio > TimeUnit.MINUTES.toMillis(4)) {
                    return Result.retry();
                }
                Appuntamento app = doc.toObject(Appuntamento.class);
                if (app == null || app.getRevisione() <= app.getSincronizzataRevisione()) continue;
                String stato = app.getStatoSincronizzazione();
                if ("email_da_verificare".equals(stato) || "errore_permanente".equals(stato)) continue;
                Lease lease = acquisisci(doc.getReference());
                if (lease == null) continue;
                String tokenUsato = null;
                try {
                    if (lease.app.getAppuntamentoPrecedenteId() != null) {
                        DocumentSnapshot precedente = attendi(db.collection("appuntamenti")
                                .document(lease.app.getAppuntamentoPrecedenteId()).get(Source.SERVER));
                        Appuntamento prima = precedente.toObject(Appuntamento.class);
                        if (prima == null) {
                            lease.stato("errore_permanente", "Prenotazione precedente non trovata. Verifica lo storico.");
                            continue;
                        }
                        if (!ProtocolloSincronizzazione.precedenteRiconciliato(prima.getStato(),
                                prima.getRevisione(), prima.getCalendarioRevisione())) {
                            lease.stato("in_attesa", "In attesa della rimozione del precedente evento Calendar.");
                            ritenta = true;
                            continue;
                        }
                    }
                    AuthorizationResult autorizzazione = attendi(Identity.getAuthorizationClient(getApplicationContext())
                            .authorize(GoogleSyncAuth.richiesta(FirebaseAuth.getInstance().getCurrentUser())));
                    if (autorizzazione.hasResolution() || autorizzazione.getAccessToken() == null) {
                        lease.stato("autorizzazione_richiesta", "Collega o autorizza l'account Google del docente.");
                        continue;
                    }
                    String token = autorizzazione.getAccessToken();
                    tokenUsato = token;
                    CalendarServiceHelper calendario = new CalendarServiceHelper(token);
                    GmailServiceHelper gmail = new GmailServiceHelper(token,
                            FirebaseAuth.getInstance().getCurrentUser().getEmail());
                    ProtocolloSincronizzazione.Email email = ProtocolloSincronizzazione.email(
                            lease.app.getRevisione(), lease.app.getEmailRevisione(), lease.app.getStatoEmail(),
                            lease.app.getSostituitoDaId() == null);
                    new SincronizzaRicevimento().esegui(
                            lease.app.getCalendarioRevisione() == lease.app.getRevisione(), email, lease,
                            new SincronizzaRicevimento.Servizi() {
                                @Override public String calendario() throws Exception {
                                    lease.verifica();
                                    return calendario.sincronizza(lease.app);
                                }
                                @Override public void email() throws Exception {
                                    lease.verifica();
                                    String esito = lease.app.getStato();
                                    gmail.invia(lease.app.getEmailStudente(), "Ricevimento DISCo: " + esito,
                                            "Gentile studente, il ricevimento con " + lease.app.getNomeProfessore()
                                            + " del " + lease.app.getData() + ", dalle " + lease.app.getOraInizio()
                                            + " alle " + lease.app.getOraFine() + ", risulta " + esito + ".");
                                }
                            });
                } catch (ApiException e) {
                    if (e.getStatusCode() == com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR
                            || e.getStatusCode() == com.google.android.gms.common.api.CommonStatusCodes.TIMEOUT) {
                        lease.stato("in_attesa", "Connessione Google non disponibile. Nuovo tentativo programmato.");
                        ritenta = true;
                    } else {
                        lease.stato("autorizzazione_richiesta", "Autorizza l'account Google dai dettagli dell'appuntamento.");
                    }
                } catch (Exception e) {
                    if (!sessione()) return Result.success();
                    Throwable causa = e instanceof SincronizzaRicevimento.InvioRifiutato ? e.getCause() : e;
                    if (causa instanceof HttpResponseException) {
                        int codice = ((HttpResponseException) causa).getStatusCode();
                        if (codice == 401 && tokenUsato != null) {
                            try { com.google.android.gms.auth.GoogleAuthUtil.clearToken(getApplicationContext(), tokenUsato); }
                            catch (Exception ignored) { /* Il consenso potrà essere rinnovato dalla UI. */ }
                        }
                        if (codice == 401 || codice == 403) {
                            // Il nuovo consenso UI potrà ottenere un token valido; nessun refresh token viene salvato.
                            lease.stato("autorizzazione_richiesta", "Autorizzazione Google scaduta o permessi insufficienti.");
                        } else if (codice >= 400 && codice < 500 && codice != 429) {
                            lease.stato("errore_permanente", "Google ha rifiutato l'operazione (" + codice + ").");
                        } else {
                            lease.stato("in_attesa", "Servizio temporaneamente non disponibile. Nuovo tentativo programmato.");
                            ritenta = true;
                        }
                    } else {
                        lease.stato("in_attesa", "Sincronizzazione interrotta. Nuovo tentativo programmato.");
                        ritenta = true;
                    }
                } finally { lease.rilascia(); }
            }
            return ritenta ? Result.retry() : Result.success();
        } catch (Exception e) {
            return sessione() ? Result.retry() : Result.success();
        }
    }

    private boolean sessione() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return !isStopped() && uid != null && user != null && uid.equals(user.getUid());
    }

    private static <T> T attendi(Task<T> task) throws Exception {
        try { return Tasks.await(task, 20, TimeUnit.SECONDS); }
        catch (ExecutionException e) {
            if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
            throw e;
        }
    }

    private Lease acquisisci(DocumentReference ref) throws Exception {
        String token = UUID.randomUUID().toString();
        return attendi(db.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(ref);
            Appuntamento app = doc.toObject(Appuntamento.class);
            Long fino = doc.getLong("syncLeaseFino");
            if (app == null || !uid.equals(app.getIdProfessore())
                    || app.getRevisione() <= app.getSincronizzataRevisione()
                    || (fino != null && fino > System.currentTimeMillis())) return null;
            // Una seconda esecuzione può aver già cambiato lo stato dopo la query iniziale.
            if ("email_da_verificare".equals(app.getStatoSincronizzazione())
                    || "errore_permanente".equals(app.getStatoSincronizzazione())) return null;
            app.setId(doc.getId());
            Map<String, Object> valori = new HashMap<>();
            valori.put("syncLeaseToken", token);
            valori.put("syncLeaseFino", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
            valori.put("statoSincronizzazione", "in_corso");
            valori.put("erroreSincronizzazione", null);
            tx.update(ref, valori);
            return new Lease(ref, app, token);
        }));
    }

    private final class Lease implements SincronizzaRicevimento.Archivio {
        final DocumentReference ref;
        final Appuntamento app;
        final String token;
        Lease(DocumentReference ref, Appuntamento app, String token) {
            this.ref = ref; this.app = app; this.token = token;
        }
        private boolean possiede(DocumentSnapshot doc) {
            Long fino = doc.getLong("syncLeaseFino");
            return token.equals(doc.getString("syncLeaseToken"))
                    && doc.getLong("revisione") != null && doc.getLong("revisione") == app.getRevisione()
                    && fino != null && fino > System.currentTimeMillis();
        }
        @Override public boolean attuale() throws Exception {
            return sessione() && possiede(attendi(ref.get(Source.SERVER)));
        }
        void verifica() throws Exception {
            if (!attuale()) throw new IllegalStateException("Aggiornamento superato o sessione terminata");
        }
        private void aggiorna(Map<String, Object> valori) throws Exception {
            if (!sessione()) throw new IllegalStateException("Sessione terminata");
            attendi(db.runTransaction(tx -> {
                if (!possiede(tx.get(ref))) throw new IllegalStateException("Aggiornamento superato");
                tx.update(ref, valori);
                return null;
            }));
        }
        void stato(String stato, String errore) throws Exception {
            Map<String, Object> valori = new HashMap<>();
            valori.put("statoSincronizzazione", stato);
            valori.put("erroreSincronizzazione", errore);
            aggiorna(valori);
        }
        @Override public void calendarioCompletato(String id) throws Exception {
            Map<String, Object> valori = new HashMap<>();
            valori.put("eventoGoogleCalendarId", id);
            valori.put("calendarioRevisione", app.getRevisione());
            aggiorna(valori);
        }
        @Override public boolean prenotaEmail() throws Exception {
            if (!sessione()) return false;
            return attendi(db.runTransaction(tx -> {
                DocumentSnapshot doc = tx.get(ref);
                if (!possiede(doc)) return false;
                Long versione = doc.getLong("emailRevisione");
                if (ProtocolloSincronizzazione.email(app.getRevisione(), versione == null ? 0 : versione,
                        doc.getString("statoEmail"), true) != ProtocolloSincronizzazione.Email.INVIA) return false;
                Map<String, Object> valori = new HashMap<>();
                valori.put("statoEmail", "in_invio");
                valori.put("emailRevisione", app.getRevisione());
                tx.update(ref, valori);
                return true;
            }));
        }
        @Override public void emailCompletata() throws Exception {
            aggiorna(java.util.Collections.singletonMap("statoEmail", "inviata"));
        }
        @Override public void emailRifiutata() throws Exception {
            aggiorna(java.util.Collections.singletonMap("statoEmail", "da_inviare"));
        }
        @Override public void emailDaVerificare() throws Exception {
            Map<String, Object> valori = new HashMap<>();
            valori.put("statoEmail", "da_verificare");
            valori.put("statoSincronizzazione", "email_da_verificare");
            valori.put("erroreSincronizzazione", "Esito email incerto: verifica la posta inviata prima di confermare la gestione.");
            aggiorna(valori);
        }
        @Override public void completa() throws Exception {
            Map<String, Object> valori = new HashMap<>();
            valori.put("sincronizzataRevisione", app.getRevisione());
            valori.put("statoSincronizzazione", "completata");
            valori.put("erroreSincronizzazione", null);
            aggiorna(valori);
        }
        void rilascia() {
            try {
                attendi(db.runTransaction(tx -> {
                    if (token.equals(tx.get(ref).getString("syncLeaseToken"))) {
                        Map<String, Object> valori = new HashMap<>();
                        valori.put("syncLeaseFino", 0L);
                        valori.put("syncLeaseToken", null);
                        tx.update(ref, valori);
                    }
                    return null;
                }));
            } catch (Exception ignored) { /* La lease scade e l'outbox resta recuperabile. */ }
        }
    }
}
