package com.hospismart.hospismartdesktop.models;


import java.time.LocalDateTime;

public class RendezVous {
    private int id;
    private LocalDateTime datetime;
    private String statut;
    private String motif;
    private int patientId;
    private int medecinId;
    private int priorite;
    private int disponibiliteId; // Lien vers la disponibilité réservée
    private String patientName; // Transient field for display

    public RendezVous() {
        this.datetime = LocalDateTime.now();
        this.statut = "EN_ATTENTE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDatetime() { return datetime; }
    public void setDatetime(LocalDateTime datetime) { this.datetime = datetime; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getPriorite() { return priorite; }
    public void setPriorite(int priorite) { this.priorite = priorite; }

    public int getDisponibiliteId() { return disponibiliteId; }
    public void setDisponibiliteId(int disponibiliteId) { this.disponibiliteId = disponibiliteId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    private String medecinName; // Transient field for patient view
    private String specialite;  // Transient field for patient view

    public String getMedecinName() { return medecinName; }
    public void setMedecinName(String medecinName) { this.medecinName = medecinName; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }
}