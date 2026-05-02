package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Service;
import com.hospismart.hospismartdesktop.services.ServiceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class PatientController implements Initializable {

    @FXML private TextField searchField;
    @FXML private HBox servicesContainer;
    @FXML private ScrollPane scrollPane;

    private ServiceService serviceService = new ServiceService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadServices();
    }

    private void loadServices() {
        try {
            List<Service> services = serviceService.findALL();
            servicesContainer.getChildren().clear();

            for (Service s : services) {
                servicesContainer.getChildren().add(createServiceCard(s));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createServiceCard(Service s) {
        VBox card = new VBox(15);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #f8faff; -fx-background-radius: 24; -fx-padding: 24; -fx-alignment: center; -fx-border-color: #eef2f6; -fx-border-radius: 24;");

        // --- GESTION DE L'IMAGE ---
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);

        String uploadPath = "C:/Users/manar/Desktop/JavaProjects/HospismartDesktop/public/uploads/services/";

        try {
            String imageName = s.getImage();
            if (imageName != null && !imageName.isEmpty()) {
                Image img = new Image("file:" + uploadPath + imageName);
                imageView.setImage(img);
            } else {
                imageView.setImage(new Image("https://ui-avatars.com/api/?name=" + s.getNom() + "&size=150&background=4178F5&color=fff"));
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement image : " + e.getMessage());
        }

        Circle clip = new Circle(75, 75, 75);
        imageView.setClip(clip);

        // --- CONTENU TEXTUEL ---
        Label title = new Label(s.getNom());
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-text-alignment: center;");
        desc.setPrefHeight(60);

        // --- BOUTON D'ACTION ---
        Button btn = new Button("Voir les spécialistes");
        btn.setStyle("-fx-background-color: #4178F5; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");

        btn.setOnAction(event -> {
            try {
                // Chargement de la vue ListeMedecins
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeMedecinsParService.fxml"));
                Parent root = loader.load();

                // Injection des données dans le MedecinController
                MedecinController controller = loader.getController();
                controller.listerMedecins(s.getId(), s.getNom());

                // Changement de scène
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) {
                System.err.println("Erreur de navigation : " + e.getMessage());
                e.printStackTrace();
            }
        });

        card.getChildren().addAll(imageView, title, desc, btn);
        return card;
    }

    @FXML
    void handleSearch() {
        String query = searchField.getText();
        System.out.println("Recherche de : " + query);
    }

    @FXML void scrollLeft() { scrollPane.setHvalue(Math.max(0, scrollPane.getHvalue() - 0.2)); }
    @FXML void scrollRight() { scrollPane.setHvalue(Math.min(1, scrollPane.getHvalue() + 0.2)); }
}