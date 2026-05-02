package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class Disponibilite {

    private int id;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean estReserve;
    private int medecinId; // Stocke l'ID brut de la base
    private User medecin; // Objet User complet (optionnel)

    // Constructeur par défaut (équivalent du __construct en PHP)
    public Disponibilite() {
        this.dateDebut = LocalDateTime.now();
        this.dateFin = LocalDateTime.now();
        this.estReserve = false;
    }

    // Constructeur paramétré (pratique pour les mocks/services)
    public Disponibilite(int id, LocalDateTime dateDebut, LocalDateTime dateFin, boolean estReserve, User medecin) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.estReserve = estReserve;
        this.medecin = medecin;
    }

    // Getters et Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    /**
     * Correction pour ton erreur précédente :
     * On définit isReserve() pour que le Controller le reconnaisse,
     * mais on garde le nom interne estReserve pour coller à ta base Symfony.
     */
    public boolean isReserve() {
        return estReserve;
    }

    public boolean isEstReserve() { // Alternative pour coller au nom PHP
        return estReserve;
    }

    public void setEstReserve(boolean estReserve) {
        this.estReserve = estReserve;
    }

    public int getMedecinId() {
        return medecinId;
    }

    public void setMedecinId(int medecinId) {
        this.medecinId = medecinId;
    }

    public User getMedecin() {
        return medecin;
    }

    public void setMedecin(User medecin) {
        this.medecin = medecin;
    }

    // Optionnel : toString pour le debug
    @Override
    public String toString() {
        return "Disponibilite{" +
                "id=" + id +
                ", dateDebut=" + dateDebut +
                ", estReserve=" + estReserve +
                '}';
    }
}