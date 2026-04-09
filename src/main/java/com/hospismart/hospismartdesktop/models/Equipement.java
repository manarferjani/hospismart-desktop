package com.hospismart.hospismartdesktop.models;


public class Equipement {
    private int id;
    private String nom;
    private String reference;
    private String etat; // Bon, Moyen, Mauvais, Défaillant
    private String relation;
    private int serviceId;

    public Equipement() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    @Override
    public String toString() { return nom + " [" + reference + "]"; }
}