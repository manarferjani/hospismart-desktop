package com.hospismart.hospismartdesktop.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    // Stage partagé accessible statiquement pour la navigation entre vues
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        // Démarrer sur le FRONTOFFICE (vue publique)
        showFrontoffice();
    }

    /** Charge et affiche le frontoffice public */
    public static void showFrontoffice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/com/hospismart/hospismartdesktop/frontoffice.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 720);
            primaryStage.setTitle("OASIS — Catalogue Public des Médicaments");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("✅ Frontoffice lancé !");
        } catch (Exception e) {
            System.err.println("❌ Erreur frontoffice : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Charge et affiche l'espace admin (backoffice) */
    public static void showAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/com/hospismart/hospismartdesktop/main.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 720);
            primaryStage.setTitle("OASIS — Backoffice Admin");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("✅ Backoffice lancé !");
        } catch (Exception e) {
            System.err.println("❌ Erreur admin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
