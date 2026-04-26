package com.hospismart.hospismartdesktop.utils;

import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.MouvementStockDAO;

import java.util.List;
import java.util.Random;

public class AddDummyMovements {
    public static void main(String[] args) {
        System.out.println("Début de l'ajout de faux mouvements de stock...");
        MedicamentDAO medDao = new MedicamentDAO();
        MouvementStockDAO mouvDao = new MouvementStockDAO();

        List<Medicament> medicaments = medDao.findAll();
        if (medicaments.isEmpty()) {
            System.out.println("Erreur: Aucun médicament trouvé dans la bdd.");
            return;
        }

        Random rand = new Random();
        int nbMovements = 25; // Nombre de faux mouvements à créer
        String[] comments = {"Réassort regulier", "Rupture de stock imminente", "Commande urgence", "Client", "Fournisseur local"};
        String[] types = {"ENTREE", "SORTIE"};

        for (int i = 0; i < nbMovements; i++) {
            Medicament m = medicaments.get(rand.nextInt(medicaments.size()));
            String type = types[rand.nextInt(types.length)];
            int qty = rand.nextInt(50) + 5; // Quantité entre 5 et 54
            String comment = comments[rand.nextInt(comments.length)];

            MouvementStock ms = new MouvementStock(type, qty, comment, m.getId());
            mouvDao.add(ms);
            System.out.println("Ajout: " + type + " de " + qty + " pour " + m.getNom());
        }

        System.out.println("Terminé avec succès!");
    }
}
