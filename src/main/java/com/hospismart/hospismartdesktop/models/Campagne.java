package com.hospismart.hospismartdesktop.models;


import java.time.LocalDateTime;

public class Campagne {
    private int id;
    private String titre;
    private String theme;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private double budget;

    public Campagne() {
        this.dateDebut = LocalDateTime.now();
        this.dateFin = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    @Override
    public String toString() { return titre + " (" + theme + ")"; }
}