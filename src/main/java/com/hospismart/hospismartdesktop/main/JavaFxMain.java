package com.hospismart.hospismartdesktop.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;

public class JavaFxMain extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // ICI : Change le chemin pour qu'il pointe vers ton fichier FXML dans resources
        // Exemple : /login.fxml ou /AjouterUser.fxml
        // Dans votre méthode start()
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Regular.ttf"), 14);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/TrouverMedecin.fxml"));
        Parent parent = fxmlLoader.load();

        // Tu peux agrandir la fenêtre (ex: 800x600)
        Scene scene = new Scene(parent);
        URL cssURL = getClass().getResource("/css/style.css");
        stage.setScene(scene);
        stage.setTitle("Hospismart - Gestion Médicale");

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}