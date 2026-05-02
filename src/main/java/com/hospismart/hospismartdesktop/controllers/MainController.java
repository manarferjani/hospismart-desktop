package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.HelloApplication;
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

/**
 * Contrôleur principal : gère la navigation entre les modules de l'application
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Afficher le dashboard par défaut au démarrage
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        loadView("/com/hospismart/hospismartdesktop/dashboard.fxml");
    }
public class MainController implements Initializable {

    @FXML
    public void showMedicaments() {
        loadView("/com/hospismart/hospismartdesktop/medicament.fxml");
    }
    @FXML private TabPane mainTabPane;
    @FXML private Tab backTab;
    @FXML private Tab frontTab;

    @FXML
    public void showCategories() {
        loadView("/com/hospismart/hospismartdesktop/categorie.fxml");
    }
    private EvenementFrontController frontController;

    @FXML
    public void showMouvements() {
        loadView("/com/hospismart/hospismartdesktop/mouvement-stock.fxml");
    }

    private void loadView(String fxmlPath) {
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            // ── Load back-office (admin) view ──
            FXMLLoader backLoader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-back.fxml"));
            Node backNode = backLoader.load();
            backTab.setContent(backNode);

            // ── Load front-office (public) view ──
            FXMLLoader frontLoader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-front.fxml"));
            Node frontNode = frontLoader.load();
            frontController = frontLoader.getController();
            frontTab.setContent(frontNode);

            // Refresh front view automatically when the user switches to it
            mainTabPane.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldTab, newTab) -> {
                        if (newTab == frontTab && frontController != null) {
                            frontController.loadData();
                        }
                    });

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement de la vue : " + fxmlPath);
            e.printStackTrace();
            System.err.println("MainController: impossible de charger les vues — " + e.getMessage());
        }
    }

    @FXML
    public void handleGoFront() {
        HelloApplication.showFrontoffice();
    }
}
