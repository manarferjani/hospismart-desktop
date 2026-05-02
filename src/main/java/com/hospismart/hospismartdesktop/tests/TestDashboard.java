package com.hospismart.hospismartdesktop.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;

public class TestDashboard extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. CHARGEMENT DES POLICES (Chemins corrigés selon votre structure de dossiers)
        // On utilise le chemin absolu depuis la racine du dossier resources
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Regular.ttf"), 14);

        // 2. CHARGEMENT DU FXML
        // D'après vos captures, ce fichier est bien à la racine de resources
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/BackDashboard.fxml"));
        Parent root = fxmlLoader.load();

        // 3. CRÉATION DE LA SCÈNE
        Scene scene = new Scene(root, 1200, 800);

        // 4. APPLICATION DU CSS (Chemin complet)
        URL cssURL = getClass().getResource("/css/style.css");

        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            System.err.println("ERREUR : Le fichier style.css est introuvable. Vérifiez le chemin !");
        }

        // Debug : Affiche les polices disponibles pour vérifier si Montserrat est bien là
        // System.out.println(Font.getFamilies());

        stage.setTitle("Test Dashboard Médecin - Hospismart");
        stage.setScene(scene);
        System.out.println("Polices disponibles : " + Font.getFamilies());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}