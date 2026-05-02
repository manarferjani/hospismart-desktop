package com.hospismart.hospismartdesktop.main;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import java.sql.SQLException;

public class MainClass {
    public static void main(String[] args) {
        // 1. Tester la connexion
        System.out.println("Test de connexion...");

        // 2. Créer un utilisateur de test
        User testUser = new User();
        testUser.setNom("Manar");
        testUser.setPrenom("Dev");
        testUser.setEmail("test@hospismart.tn");
        testUser.setType("ADMIN");

        UserService us = new UserService();

        try {
            // 3. Tester l'insertion
            us.insertOne(testUser);
            System.out.println("Insertion réussie !");

            // 4. Tester la récupération
            System.out.println("Liste des utilisateurs : " + us.findALL());

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }
}