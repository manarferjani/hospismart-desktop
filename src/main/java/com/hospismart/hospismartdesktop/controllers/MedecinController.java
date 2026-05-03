package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MedecinController {

    @FXML private FlowPane doctorsGrid;
    @FXML private Label labelTitre;
    @FXML private Label labelCount;

    private UserService userService = new UserService();

    /**
     * Remplit la grille avec les médecins du service sélectionné
     */
    public void listerMedecins(int serviceId, String serviceNom) {
        if (doctorsGrid == null) return;

        doctorsGrid.getChildren().clear();
        labelTitre.setText("Nos spécialistes en " + serviceNom);

        try {
            // Récupération des données via le service (Base de données)
            List<User> medecins = userService.findMedecinsByService(serviceId);

            labelCount.setText(medecins.size() + " expert(s) disponible(s)");

            for (User m : medecins) {
                // Création dynamique de la carte pour chaque médecin
                VBox card = creerCardMedecin(
                        m.getId(),
                        m.getNom(),
                        m.getPrenom(),
                        m.getSpecialite(),
                        m.getImage(),
                        serviceId,
                        serviceNom
                );
                doctorsGrid.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des médecins : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée le composant visuel (Card) pour un médecin
     */
    private VBox creerCardMedecin(int id, String nom, String prenom, String specialite, String imageName, int serviceId, String serviceNom) {
        // === 1. CONTENEUR PRINCIPAL ===
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(350);
        card.setPrefHeight(500); // Hauteur augmentée pour loger les deux boutons
        card.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-padding: 35; " +
                "-fx-border-color: #eef2f6; -fx-border-width: 1; -fx-border-radius: 30;");

        DropShadow shadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.07));
        card.setEffect(shadow);

        // === 2. AVATAR CIRCULAIRE (Logique de recadrage conservée) ===
        ImageView imageView = new ImageView();
        double displaySize = 180;
        imageView.setFitWidth(displaySize);
        imageView.setFitHeight(displaySize);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        String baseDir = System.getProperty("user.dir").replace("\\", "/");
        String uploadPathMedecins = baseDir + "/public/uploads/medecins/";

        try {
            if (imageName != null && !imageName.isEmpty()) {
                Image img = new Image("file:" + uploadPathMedecins + imageName, 0, 360, true, true);
                if (img.isError()) throw new Exception("Image introuvable");
                imageView.setImage(img);

                if (img.getWidth() > img.getHeight()) {
                    double scale = displaySize / img.getHeight();
                    double croppedWidth = displaySize / scale;
                    imageView.setViewport(new Rectangle2D((img.getWidth() - croppedWidth) / 2, 0, croppedWidth, img.getHeight()));
                } else {
                    double scale = displaySize / img.getWidth();
                    double croppedHeight = displaySize / scale;
                    imageView.setViewport(new Rectangle2D(0, (img.getHeight() - croppedHeight) / 3, img.getWidth(), croppedHeight));
                }
            } else {
                String encodedName = (nom + "+" + prenom).replace(" ", "+");
                imageView.setImage(new Image("https://ui-avatars.com/api/?name=" + encodedName + "&background=EBF2FF&color=4178F5&size=180&bold=true", displaySize, displaySize, false, true));
            }
        } catch (Exception e) {
            String encodedName = (nom + "+" + prenom).replace(" ", "+");
            imageView.setImage(new Image("https://ui-avatars.com/api/?name=" + encodedName + "&background=ffdede&color=e03e3e&size=180&bold=true", displaySize, displaySize, false, true));
        }

        Circle clip = new Circle(displaySize/2, displaySize/2, displaySize/2);
        imageView.setClip(clip);

        StackPane avatarContainer = new StackPane(imageView);
        avatarContainer.setMaxSize(displaySize + 10, displaySize + 10);
        avatarContainer.setStyle("-fx-border-color: #f0f4ff; -fx-border-width: 5; -fx-border-radius: 200;");

        // === 3. INFOS DU MÉDECIN ===
        Label nameLabel = new Label("Dr. " + nom.toUpperCase() + " " + prenom);
        nameLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        Label specLabel = new Label(specialite != null ? specialite : "Généraliste");
        specLabel.setStyle("-fx-text-fill: #4178F5; -fx-font-weight: 600; -fx-font-size: 16;");

        VBox textContainer = new VBox(8);
        textContainer.setAlignment(Pos.CENTER);
        textContainer.getChildren().addAll(nameLabel, specLabel);

        // === 4. BOUTONS (SECTION CORRIGÉE) ===
        VBox actionContainer = new VBox(10); // Conteneur pour empiler les boutons avec espacement
        actionContainer.setAlignment(Pos.CENTER);

        Button btnProfil = new Button("Voir profil");
        btnProfil.setMaxWidth(Double.MAX_VALUE);
        btnProfil.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a1a2e; -fx-border-color: #1a1a2e; " +
                "-fx-border-radius: 12; -fx-padding: 12; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnRdv = new Button("Prendre rendez-vous");
        btnRdv.setMaxWidth(Double.MAX_VALUE);
        btnRdv.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 12; " +
                "-fx-padding: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14;");

        btnRdv.setOnAction(event -> {
            try {
                // 1. Chargez le FXML en tant que Parent ou VBox (le type exact de la racine du FXML)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/priseRendezvous.fxml"));
                VBox rootPage = loader.load(); // CHANGÉ : ScrollPane -> VBox

                // 2. Récupérez le contrôleur
                RendezVousController controller = loader.getController();

                // 3. Passez les arguments (incluant l'ID)
                controller.setMedecinData(id, nom, prenom, specialite, imageName, serviceId, serviceNom);

                // 4. Navigation
                Scene currentScene = btnRdv.getScene();
                Node contentArea = currentScene.lookup("#mainContent");

                if (contentArea instanceof Pane) {
                    ((Pane) contentArea).getChildren().setAll(rootPage);
                } else {
                    // Si vous n'avez pas de mainContent, on change la racine de la fenêtre
                    currentScene.setRoot(rootPage);
                }

            } catch (IOException e) {
                System.err.println("Erreur de navigation : " + e.getMessage());
                e.printStackTrace();
            }
        });


        actionContainer.setAlignment(Pos.CENTER);
        actionContainer.getChildren().addAll(btnProfil, btnRdv);

        // === 5. ANIMATIONS ET AJOUT FINAL ===
        card.setOnMouseEntered(e -> card.setTranslateY(-10));
        card.setOnMouseExited(e -> card.setTranslateY(0));


        btnProfil.setOnMouseEntered(e -> {
            btnProfil.setStyle("-fx-background-color: #f0f4ff; -fx-text-fill: #4178F5; -fx-border-color: #4178F5; " +
                    "-fx-border-radius: 12; -fx-padding: 12; -fx-font-weight: bold; -fx-cursor: hand;");
        });
        btnProfil.setOnMouseExited(e -> {
            btnProfil.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a1a2e; -fx-border-color: #1a1a2e; " +
                    "-fx-border-radius: 12; -fx-padding: 12; -fx-font-weight: bold; -fx-cursor: hand;");
        });


        btnRdv.setOnMouseEntered(e -> {
            btnRdv.setStyle("-fx-background-color: #4178F5; -fx-text-fill: white; -fx-background-radius: 12; " +
                    "-fx-padding: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14;");
        });
        btnRdv.setOnMouseExited(e -> {
            btnRdv.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 12; " +
                    "-fx-padding: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14;");
        });



        card.getChildren().addAll(avatarContainer, textContainer, actionContainer);

        return card;
    }
    @FXML
    private void handleBack(javafx.event.ActionEvent event) {
        try {
            // Code pour revenir à la vue précédente (PatientView par exemple)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TrouverMedecin.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);

        } catch (java.io.IOException e) {
            System.err.println("Erreur lors du retour en arrière : " + e.getMessage());
            e.printStackTrace();
        }
    }
}