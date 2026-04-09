package com.hospismart.hospismartdesktop.models;


public class Diagnostic {
    private int id;
    private String contenu;
    private double probabiliteIa;

    public Diagnostic() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public double getProbabiliteIa() { return probabiliteIa; }
    public void setProbabiliteIa(double probabiliteIa) { this.probabiliteIa = probabiliteIa; }
}