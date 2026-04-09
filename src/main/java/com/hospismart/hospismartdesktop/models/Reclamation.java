package com.hospismart.hospismartdesktop.models;


import java.time.LocalDateTime;

public class Reclamation {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateCreation;
    private String email;
    private String nomPatient;
    private String statut;
    private String categorie;
    private String priorite;

    public Reclamation() {
        this.dateCreation = LocalDateTime.now();
        this.statut = "En attente";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNomPatient() { return nomPatient; }
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
}
