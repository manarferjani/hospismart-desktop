package com.hospismart.hospismartdesktop.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ModernModalController {
    @FXML private Label lblTitle, lblMessage;
    @FXML private Button btnPrimary, btnSecondary;
    @FXML private StackPane iconContainer;
    @FXML private ImageView iconImage;

    private Runnable onConfirm;

    public void setData(String title, String message, boolean isError, Runnable onConfirm) {
        lblTitle.setText(title);
        lblMessage.setText(message);
        this.onConfirm = onConfirm;

        // Forcer le bouton à s'adapter au contenu pour éviter les "..."
        btnPrimary.setMinWidth(Button.USE_PREF_SIZE);
        if (btnSecondary != null) btnSecondary.setMinWidth(Button.USE_PREF_SIZE);

        if (isError) {
            iconContainer.getStyleClass().setAll("icon-box-error");
            btnPrimary.setText("Refuser");
            btnPrimary.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 10 20;");
        } else {
            iconContainer.getStyleClass().setAll("icon-box-success");
            btnPrimary.setText("Accepter");
            btnPrimary.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 10 20;");
        }

        // Chargement de l'icône
        try {
            String imageName = isError ? "cancel.png" : "check.png";
            Image img = new Image(getClass().getResourceAsStream("/icons/" + imageName));
            iconImage.setImage(img);
        } catch (Exception e) {
            System.err.println("Erreur chargement icône modal");
        }
    }

    public void showConfirmMode() {
        btnSecondary.setVisible(true);
    }

    @FXML
    private void onPrimaryAction() {
        if (onConfirm != null) onConfirm.run();
        closeModal();
    }

    @FXML
    private void closeModal() {
        ((Stage) lblTitle.getScene().getWindow()).close();
    }
}