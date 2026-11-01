package com.simone.discounimib.models;

import com.google.firebase.Timestamp;

@com.google.firebase.firestore.IgnoreExtraProperties
public class Appuntamento {
    private String id;
    private String idProfessore;
    private String idStudente;
    private String data;
    private String oraInizio;
    private String oraFine;
    private String motivazione;
    private String stato; // sospeso, accettato, rifiutato, annullato
    private Timestamp dataCreazione;
    private Timestamp inizioIstante;
    private Timestamp fineIstante;
    private String emailProfessore;
    private String emailStudente;
    private String nomeProfessore;
    private String nomeStudente;
    private String eventoGoogleCalendarId;

    private long revisione;
    private long sincronizzataRevisione;
    private long calendarioRevisione;
    private long emailRevisione;
    private String statoSincronizzazione;
    private String erroreSincronizzazione;
    private String statoEmail;
    private String appuntamentoPrecedenteId;
    private String sostituitoDaId;

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

    public Timestamp getInizioIstante() { return inizioIstante; }
    public void setInizioIstante(Timestamp value) { inizioIstante = value; }
    public Timestamp getFineIstante() { return fineIstante; }
    public void setFineIstante(Timestamp value) { fineIstante = value; }

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
    public long getRevisione() { return revisione; }
    public void setRevisione(long value) { revisione = value; }
    public long getSincronizzataRevisione() { return sincronizzataRevisione; }
    public void setSincronizzataRevisione(long value) { sincronizzataRevisione = value; }
    public long getCalendarioRevisione() { return calendarioRevisione; }
    public void setCalendarioRevisione(long value) { calendarioRevisione = value; }
    public long getEmailRevisione() { return emailRevisione; }
    public void setEmailRevisione(long value) { emailRevisione = value; }
    public String getStatoSincronizzazione() { return statoSincronizzazione; }
    public void setStatoSincronizzazione(String value) { statoSincronizzazione = value; }
    public String getErroreSincronizzazione() { return erroreSincronizzazione; }
    public void setErroreSincronizzazione(String value) { erroreSincronizzazione = value; }
    public String getStatoEmail() { return statoEmail; }
    public void setStatoEmail(String value) { statoEmail = value; }
    public String getAppuntamentoPrecedenteId() { return appuntamentoPrecedenteId; }
    public void setAppuntamentoPrecedenteId(String value) { appuntamentoPrecedenteId = value; }
    public String getSostituitoDaId() { return sostituitoDaId; }
    public void setSostituitoDaId(String value) { sostituitoDaId = value; }
}
