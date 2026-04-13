package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.HelloApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Contrôleur principal : gère la navigation entre les modules de l'application
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Afficher médicaments par défaut au démarrage
        showMedicaments();
    }

    @FXML
    public void showMedicaments() {
        loadView("/com/hospismart/hospismartdesktop/medicament.fxml");
    }

    @FXML
    public void showCategories() {
        loadView("/com/hospismart/hospismartdesktop/categorie.fxml");
    }

    @FXML
    public void showMouvements() {
        loadView("/com/hospismart/hospismartdesktop/mouvement-stock.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement de la vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoFront() {
        HelloApplication.showFrontoffice();
    }
}
