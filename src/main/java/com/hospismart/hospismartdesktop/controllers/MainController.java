package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.JavaFxMain; // Import corrigé
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea; // Zone où s'affichent les pages du menu

    // --- Navigation Menu Gauche ---

    @FXML
    public void showDashboard() {
        loadView("/com/hospismart/hospismartdesktop/dashboard.fxml");
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

    @FXML
    public void showUsers() {
        loadView("/com/hospismart/hospismartdesktop/BackOfficeUsers.fxml");
    }

    @FXML
    public void showReclamations() {
        loadView("/com/hospismart/hospismartdesktop/reclamation_back.fxml");
    }

    @FXML
    public void showEvenements() {
        loadView("/com/hospismart/hospismartdesktop/evenement-back.fxml");
    }

    // Méthode générique pour charger une vue dans le contentArea
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

    // --- Initialisation et Onglets ---

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Au démarrage, afficher le dashboard par défaut
        showDashboard();
    }

    @FXML
    public void handleGoFront() {
        // Appelle la méthode static de ta classe de lancement
        JavaFxMain.showFrontoffice();
    }

    @FXML
    public void handleLogout(javafx.event.ActionEvent event) {
        System.out.println("[Navigation] Déconnexion depuis le Back-Office (MainController)");
        com.hospismart.hospismartdesktop.utils.Session.getInstance().cleanUserSession();
        JavaFxMain.setRoot("/com/hospismart/hospismartdesktop/Login.fxml", "Hospismart - Connexion");
    }
}