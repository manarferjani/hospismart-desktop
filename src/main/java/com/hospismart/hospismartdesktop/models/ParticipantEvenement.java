package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class ParticipantEvenement {
    private int id;
    private int evenementId; // ID de l'objet Evenement
    private int participantId; // ID de l'objet User
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role; // organisateur, intervenant, participant, observateur
    private boolean confirmePresence;
    private LocalDateTime dateConfirmation;

    public ParticipantEvenement() {
        this.role = "participant";
        this.confirmePresence = false;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public int getParticipantId() { return participantId; }
    public void setParticipantId(int participantId) { this.participantId = participantId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isConfirmePresence() { return confirmePresence; }
    public void setConfirmePresence(boolean confirmePresence) { this.confirmePresence = confirmePresence; }

    public LocalDateTime getDateConfirmation() { return dateConfirmation; }
    public void setDateConfirmation(LocalDateTime dateConfirmation) { this.dateConfirmation = dateConfirmation; }

    @Override
    public String toString() {
        return prenom + " " + nom + " (" + role + ")";
    }
}