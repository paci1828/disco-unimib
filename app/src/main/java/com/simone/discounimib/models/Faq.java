package com.simone.discounimib.models;

public class Faq {
    private String id;
    private String domanda;
    private String risposta;
    private int ordine;

    public Faq() {
        // Costruttore vuoto per Firestore
    }

    public Faq(String id, String domanda, String risposta, int ordine) {
        this.id = id;
        this.domanda = domanda;
        this.risposta = risposta;
        this.ordine = ordine;
    }

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDomanda() { return domanda; }
    public void setDomanda(String domanda) { this.domanda = domanda; }

    public String getRisposta() { return risposta; }
    public void setRisposta(String risposta) { this.risposta = risposta; }

    public int getOrdine() { return ordine; }
    public void setOrdine(int ordine) { this.ordine = ordine; }
}