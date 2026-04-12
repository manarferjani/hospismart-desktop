package com.hospismart.hospismartdesktop.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeController {

    @FXML
    private void onBackOffice(MouseEvent event) {
        loadView(event, "/com/hospismart/hospismartdesktop/evenement-back.fxml",
                "HospiSmart — Administration (Back Office)");
    }

    @FXML
    private void onFrontOffice(MouseEvent event) {
        loadView(event, "/com/hospismart/hospismartdesktop/evenement-front.fxml",
                "HospiSmart — Espace Public (Front Office)");
    }

    private void loadView(MouseEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1200, 760);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("WelcomeController: impossible de charger la vue — " + e.getMessage());
        }
    }
}
