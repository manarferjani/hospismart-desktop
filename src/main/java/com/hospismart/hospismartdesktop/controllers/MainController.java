package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.JavaFxMain; // Import corrigé
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TabPane mainTabPane;
    @FXML private Tab backTab;
    @FXML private Tab frontTab;
    @FXML private StackPane contentArea; // Zone où s'affichent les pages du menu

    private EvenementFrontController frontController;

    // --- Navigation Menu Gauche ---

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
        try {
            // Chargement auto du back-office dans son onglet
            FXMLLoader backLoader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/evenement-back.fxml"));
            backTab.setContent(backLoader.load());

            // Chargement auto du front-office dans son onglet
            FXMLLoader frontLoader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/evenement-front.fxml"));
            frontTab.setContent(frontLoader.load());
            frontController = frontLoader.getController();

            // Rafraîchir les données quand on clique sur l'onglet Front
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == frontTab && frontController != null) {
                    frontController.loadData();
                }
            });

        } catch (IOException e) {
            System.err.println("❌ Erreur initialisation MainController");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoFront() {
        // Appelle la méthode static de ta classe de lancement
        JavaFxMain.showFrontoffice();
    }
}