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
}