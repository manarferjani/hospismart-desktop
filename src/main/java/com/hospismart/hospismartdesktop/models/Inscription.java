package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class Inscription {

    private int id;
    private int evenementId;
    private String nomParticipant;
    private String emailParticipant;
    private String telephoneParticipant;
    private LocalDateTime dateInscription;

    public Inscription() {
        this.dateInscription = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public String getNomParticipant() { return nomParticipant; }
    public void setNomParticipant(String nomParticipant) { this.nomParticipant = nomParticipant; }

    public String getEmailParticipant() { return emailParticipant; }
    public void setEmailParticipant(String emailParticipant) { this.emailParticipant = emailParticipant; }

    public String getTelephoneParticipant() { return telephoneParticipant; }
    public void setTelephoneParticipant(String telephoneParticipant) { this.telephoneParticipant = telephoneParticipant; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    @Override
    public String toString() {
        return nomParticipant + " (" + emailParticipant + ")";
    }
}
