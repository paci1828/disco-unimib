package com.simone.discounimib.models;

import com.google.firebase.Timestamp;

public class User {
    private String uid;
    private String email;
    private String nome;
    private String cognome;
    private String ruolo; // "studente" o "professore"
    private String matricola;
    private Timestamp dataCreazione;
    private Timestamp ultimoAccesso;

    public User() {
        // Costruttore vuoto per Firestore
    }

    public User(String uid, String email, String nome, String cognome, String ruolo, String matricola) {
        this.uid = uid;
        this.email = email;
        this.nome = nome;
        this.cognome = cognome;
        this.ruolo = ruolo;
        this.matricola = matricola;
    }

    // Getter e Setter
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getRuolo() { return ruolo; }
    public void setRuolo(String ruolo) { this.ruolo = ruolo; }

    public String getMatricola() { return matricola; }
    public void setMatricola(String matricola) { this.matricola = matricola; }

    public Timestamp getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Timestamp dataCreazione) { this.dataCreazione = dataCreazione; }

    public Timestamp getUltimoAccesso() { return ultimoAccesso; }
    public void setUltimoAccesso(Timestamp ultimoAccesso) { this.ultimoAccesso = ultimoAccesso; }
}