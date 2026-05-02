package com.hospismart.hospismartdesktop.models; // Package du modele Reponse

import java.time.LocalDateTime; // Type date/heure pour stocker la date de reponse

public class Reponse { // Classe modele qui represente une reponse admin
    private int id; // Identifiant unique de la reponse
    private String contenu; // Texte de la reponse
    private LocalDateTime dateReponse; // Date et heure de creation/reponse
    private String adminNom; // Nom de l'administrateur qui repond
    private int reclamationId; // ID de la reclamation cible
    private String adminEmail; // Email de l'administrateur

    public Reponse() { // Constructeur par defaut
        this.dateReponse = LocalDateTime.now(); // Initialise la date au moment courant
    }

    public int getId() { return id; } // Retourne l'id
    public void setId(int id) { this.id = id; } // Met a jour l'id

    public String getContenu() { return contenu; } // Retourne le contenu
    public void setContenu(String contenu) { this.contenu = contenu; } // Met a jour le contenu

    public LocalDateTime getDateReponse() { return dateReponse; } // Retourne la date de reponse
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; } // Met a jour la date de reponse

    public String getAdminNom() { return adminNom; } // Retourne le nom admin
    public void setAdminNom(String adminNom) { this.adminNom = adminNom; } // Met a jour le nom admin

    public int getReclamationId() { return reclamationId; } // Retourne l'id reclamation liee
    public void setReclamationId(int reclamationId) { this.reclamationId = reclamationId; } // Met a jour l'id reclamation liee

    public String getAdminEmail() { return adminEmail; } // Retourne l'email admin
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; } // Met a jour l'email admin
} // Fin de la classe Reponse
