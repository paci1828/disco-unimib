package com.simone.discounimib.models;

public class Professore extends User {
    private String giornoRicevimento;
    private String oraInizio;
    private String oraFine;
    private int durataAppuntamento; // in minuti

    public Professore() {
        super();
    }

    public Professore(String uid, String email, String nome, String cognome, String ruolo, String matricola,
                     String giornoRicevimento, String oraInizio, String oraFine, int durataAppuntamento) {
        super(uid, email, nome, cognome, ruolo, matricola);
        this.giornoRicevimento = giornoRicevimento;
        this.oraInizio = oraInizio;
        this.oraFine = oraFine;
        this.durataAppuntamento = durataAppuntamento;
    }

    // Getter e Setter
    public String getGiornoRicevimento() { return giornoRicevimento; }
    public void setGiornoRicevimento(String giornoRicevimento) { this.giornoRicevimento = giornoRicevimento; }

    public String getOraInizio() { return oraInizio; }
    public void setOraInizio(String oraInizio) { this.oraInizio = oraInizio; }

    public String getOraFine() { return oraFine; }
    public void setOraFine(String oraFine) { this.oraFine = oraFine; }

    public int getDurataAppuntamento() { return durataAppuntamento; }
    public void setDurataAppuntamento(int durataAppuntamento) { this.durataAppuntamento = durataAppuntamento; }
}