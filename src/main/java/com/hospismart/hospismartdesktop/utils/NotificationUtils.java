package com.hospismart.hospismartdesktop.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

public class NotificationUtils {

    public static void showSuccess(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message, "success-alert");
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message, "error-alert");
    }

    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message, "warning-alert");
    }

    private static void showAlert(Alert.AlertType type, String title, String message, String styleClass) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();

        try {
            // Nouveau chemin basé sur votre arborescence (resources/css/modals.css)
            String cssPath = "/css/modals.css";
            java.net.URL resource = NotificationUtils.class.getResource(cssPath);

            if (resource != null) {
                dialogPane.getStylesheets().add(resource.toExternalForm());
                dialogPane.getStyleClass().add("modern-alert");
                dialogPane.getStyleClass().add(styleClass);
            } else {
                System.err.println("Fichier CSS introuvable au chemin : " + cssPath);
            }
        } catch (Exception e) {
            System.err.println("Erreur style : " + e.getMessage());
        }

        alert.showAndWait();
    }
}