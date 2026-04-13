package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

/**
 * Modèle MouvementStock - correspond à la table mouvement_stock
 * Structure BDD : id, type (ENTREE/SORTIE), quantite, date_mouvement, commentaire, medicament_id
 */
public class MouvementStock {
    private int id;
    private String type;           // "ENTREE" ou "SORTIE"
    private int quantite;
    private LocalDateTime dateMouvement;
    private String commentaire;
    private int medicamentId;

    // Champ calculé (non en BDD) pour affichage dans le tableau
    private String medicamentNom;

    public MouvementStock() {}

    public MouvementStock(String type, int quantite, String commentaire, int medicamentId) {
        this.type = type;
        this.quantite = quantite;
        this.commentaire = commentaire;
        this.medicamentId = medicamentId;
        this.dateMouvement = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public int getMedicamentId() { return medicamentId; }
    public void setMedicamentId(int medicamentId) { this.medicamentId = medicamentId; }

    public String getMedicamentNom() { return medicamentNom; }
    public void setMedicamentNom(String medicamentNom) { this.medicamentNom = medicamentNom; }

    @Override
    public String toString() {
        return type + " (" + quantite + ") - " + (medicamentNom != null ? medicamentNom : medicamentId);
    }
}
