package com.simone.discounimib.models;

import com.google.firebase.Timestamp;

public class Appuntamento {
    private String id;
    private String idProfessore;
    private String idStudente;
    private String data;
    private String oraInizio;
    private String oraFine;
    private String motivazione;
    private String stato; // "sospeso", "accettato", "rifiutato"
    private Timestamp dataCreazione;
    private String emailProfessore;
    private String emailStudente;
    private String nomeProfessore;
    private String nomeStudente;
    private String eventoGoogleCalendarId;

    public Appuntamento() {
        // Costruttore vuoto per Firestore
    }

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdProfessore() { return idProfessore; }
    public void setIdProfessore(String idProfessore) { this.idProfessore = idProfessore; }

    public String getIdStudente() { return idStudente; }
    public void setIdStudente(String idStudente) { this.idStudente = idStudente; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getOraInizio() { return oraInizio; }
    public void setOraInizio(String oraInizio) { this.oraInizio = oraInizio; }

    public String getOraFine() { return oraFine; }
    public void setOraFine(String oraFine) { this.oraFine = oraFine; }

    public String getMotivazione() { return motivazione; }
    public void setMotivazione(String motivazione) { this.motivazione = motivazione; }

    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }

    public Timestamp getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Timestamp dataCreazione) { this.dataCreazione = dataCreazione; }

    public String getEmailProfessore() { return emailProfessore; }
    public void setEmailProfessore(String emailProfessore) { this.emailProfessore = emailProfessore; }

    public String getEmailStudente() { return emailStudente; }
    public void setEmailStudente(String emailStudente) { this.emailStudente = emailStudente; }

    public String getNomeProfessore() { return nomeProfessore; }
    public void setNomeProfessore(String nomeProfessore) { this.nomeProfessore = nomeProfessore; }

    public String getNomeStudente() { return nomeStudente; }
    public void setNomeStudente(String nomeStudente) { this.nomeStudente = nomeStudente; }

    public String getEventoGoogleCalendarId() { return eventoGoogleCalendarId; }
    public void setEventoGoogleCalendarId(String eventoGoogleCalendarId) { this.eventoGoogleCalendarId = eventoGoogleCalendarId; }
}