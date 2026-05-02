package com.hospismart.hospismartdesktop.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class LauncherController {

    @FXML
    private Button btnAdmin;

    @FXML
    private Button btnClient;

    @FXML
    void ouvrirBackOffice(ActionEvent event) {
        ouvrirFenetre("/com/hospismart/hospismartdesktop/reclamation_back.fxml", "Hospismart - Espace Admin", 1000, 700, btnAdmin);
    }

    @FXML
    void ouvrirFrontOffice(ActionEvent event) {
        ouvrirFenetre("/com/hospismart/hospismartdesktop/reclamation_front.fxml", "Hospismart - Espace Client", 800, 600, btnClient);
    }

    private void ouvrirFenetre(String fxmlPath, String title, int width, int height, Button sourceBtn) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(fxmlLoader.load());

            // Ouvrir une nouvelle fenêtre
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);

            // Rendre plein écran (Maximized) si c'est le Front Office, ou utiliser la taille par défaut
            if (title.contains("Client")) {
                stage.setMaximized(true);
            } else {
                stage.setWidth(width);
                stage.setHeight(height);
            }

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'ouverture de : " + fxmlPath);
        }
    }
}
