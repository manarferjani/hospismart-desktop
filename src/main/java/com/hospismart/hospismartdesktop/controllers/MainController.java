package com.hospismart.hospismartdesktop.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TabPane mainTabPane;
    @FXML private Tab backTab;
    @FXML private Tab frontTab;

    private EvenementFrontController frontController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
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
            e.printStackTrace();
            System.err.println("MainController: impossible de charger les vues — " + e.getMessage());
        }
    }
}
