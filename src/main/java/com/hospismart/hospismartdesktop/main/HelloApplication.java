package com.hospismart.hospismartdesktop.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/hospismart/hospismartdesktop/welcome-view.fxml"));
        Scene scene = new Scene(loader.load(), 900, 620);
        scene.getStylesheets().add(
                getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());
        stage.setTitle("HospiSmart — Bienvenue");
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.show();
    }
}

