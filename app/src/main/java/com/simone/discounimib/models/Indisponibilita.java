package com.simone.discounimib.models;

public class Indisponibilita {
    private String id;
    private String idProfessore;
    private String dataInizio;
    private String dataFine;
    private String oraInizio;
    private String oraFine;
    private String motivo;

    public Indisponibilita() {
        // Costruttore vuoto per Firestore
    }

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdProfessore() { return idProfessore; }
    public void setIdProfessore(String idProfessore) { this.idProfessore = idProfessore; }

    public String getDataInizio() { return dataInizio; }
    public void setDataInizio(String dataInizio) { this.dataInizio = dataInizio; }

    public String getDataFine() { return dataFine; }
    public void setDataFine(String dataFine) { this.dataFine = dataFine; }

    public String getOraInizio() { return oraInizio; }
    public void setOraInizio(String oraInizio) { this.oraInizio = oraInizio; }

    public String getOraFine() { return oraFine; }
    public void setOraFine(String oraFine) { this.oraFine = oraFine; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}