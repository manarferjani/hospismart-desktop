package com.hospismart.hospismartdesktop.models; // Package du modele Reclamation


import java.time.LocalDateTime; // Type date/heure utilise pour la date de creation

public class Reclamation { // Classe modele representant une reclamation client
    private int id; // Identifiant unique de la reclamation
    private String titre; // Titre court de la reclamation
    private String description; // Description detaillee du probleme
    private LocalDateTime dateCreation; // Date et heure de creation
    private String email; // Email du client
    private String nomPatient; // Nom du patient/client
    private String statut; // Statut de traitement (ex: En attente, En cours, Resolu)
    private String categorie; // Categorie metier de la reclamation
    private String priorite; // Priorite de traitement
    private String reponse; // Ancien champ texte reponse (si utilise)
    private String etatMental; // Etat mental predit du client

    public Reclamation() { // Constructeur par defaut
        this.dateCreation = LocalDateTime.now(); // Initialise la date au moment courant
        this.statut = "En attente"; // Initialise un statut par defaut
    }

    public int getId() { return id; } // Retourne l'id
    public void setId(int id) { this.id = id; } // Met a jour l'id

    public String getTitre() { return titre; } // Retourne le titre
    public void setTitre(String titre) { this.titre = titre; } // Met a jour le titre

    public String getDescription() { return description; } // Retourne la description
    public void setDescription(String description) { this.description = description; } // Met a jour la description

    public LocalDateTime getDateCreation() { return dateCreation; } // Retourne la date de creation
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; } // Met a jour la date de creation

    public String getEmail() { return email; } // Retourne l'email client
    public void setEmail(String email) { this.email = email; } // Met a jour l'email client

    public String getNomPatient() { return nomPatient; } // Retourne le nom du patient
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; } // Met a jour le nom du patient

    public String getStatut() { return statut; } // Retourne le statut
    public void setStatut(String statut) { this.statut = statut; } // Met a jour le statut

    public String getCategorie() { return categorie; } // Retourne la categorie
    public void setCategorie(String categorie) { this.categorie = categorie; } // Met a jour la categorie

    public String getPriorite() { return priorite; } // Retourne la priorite
    public void setPriorite(String priorite) { this.priorite = priorite; } // Met a jour la priorite

    public String getReponse() { return reponse; } // Retourne la reponse texte
    public void setReponse(String reponse) { this.reponse = reponse; } // Met a jour la reponse texte

    public String getEtatMental() { return etatMental; } // Retourne l'etat mental
    public void setEtatMental(String etatMental) { this.etatMental = etatMental; } // Met a jour l'etat mental
} // Fin de la classe Reclamation
