package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class Evenement {

    private int id;
    private String titre;
    private String description;
    private String typeEvenement; // réunion, formation, visite, maintenance, autre
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String statut; // planifié, en_cours, terminé, annulé
    private double budgetAlloue;
    private Double latitude;
    private Double longitude;

    public Evenement() {
        this.statut = "planifié";
        this.typeEvenement = "réunion";
        this.dateDebut = LocalDateTime.now();
        this.dateFin = LocalDateTime.now().plusHours(2);
        this.budgetAlloue = 0.0;
    }

    // ── Getters & Setters ──────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(String typeEvenement) { this.typeEvenement = typeEvenement; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getBudgetAlloue() { return budgetAlloue; }
    public void setBudgetAlloue(double budgetAlloue) { this.budgetAlloue = budgetAlloue; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return titre + " (" + typeEvenement + ")";
    }
}
