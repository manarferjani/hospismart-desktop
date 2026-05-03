package com.hospismart.hospismartdesktop.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import java.io.IOException;
import com.hospismart.hospismartdesktop.models.Disponibilite;
import com.hospismart.hospismartdesktop.services.DisponibiliteService;
import javafx.scene.control.Button;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class RendezVousController {

    @FXML private ImageView imgMedecin;
    @FXML private Label lblNomMedecin;
    @FXML private Label lblSpecialite;
    @FXML private FlowPane gridDispo;
    @FXML private Label lblCount;
    @FXML private Hyperlink btnRetour;

    // --- VARIABLES AJOUTÉES POUR MÉMORISER LE SERVICE ---
    private int currentServiceId;
    private String currentServiceName;
    private int medecinId;
    private final DisponibiliteService disponibiliteService = new DisponibiliteService();

    /**
     * Cette méthode doit être appelée par le MedecinController lors du clic sur une carte.
     * Notez l'ajout de serviceId et serviceNom à la fin des paramètres.
     */
    public void setMedecinData(int medecinId, String nom, String prenom, String specialite, String imageName, int serviceId, String serviceNom) {
        this.medecinId = medecinId;
        // Sauvegarde des données pour le retour
        this.currentServiceId = serviceId;
        this.currentServiceName = serviceNom;

        // Remplissage des textes
        lblNomMedecin.setText("DR. " + nom.toUpperCase() + " " + prenom);
        lblSpecialite.setText(specialite != null ? specialite : "Généraliste");

        // --- CORRECTION DE L'IMAGE ---
        double displaySize = 150; // La taille que vous voulez afficher (carré)
        imgMedecin.setFitWidth(displaySize);
        imgMedecin.setFitHeight(displaySize);
        imgMedecin.setPreserveRatio(true);
        imgMedecin.setSmooth(true);

        try {
            String baseDir = System.getProperty("user.dir").replace("\\", "/");
            String path = "file:" + baseDir + "/public/uploads/medecins/" + imageName;
            // On charge l'image en grand pour avoir de la qualité
            Image img = new Image(path, 0, 400, true, true);

            if (img.isError()) throw new Exception("Erreur");

            imgMedecin.setImage(img);

            // --- LOGIQUE DE RECADRAGE INTELLIGENT (CROP) ---
            // On doit calculer le viewport pour ne garder que le centre carré de l'image
            if (img.getWidth() > img.getHeight()) {
                // L'image est plus large que haute (Paysage)
                double scale = displaySize / img.getHeight();
                double croppedWidth = displaySize / scale;
                imgMedecin.setViewport(new javafx.geometry.Rectangle2D((img.getWidth() - croppedWidth) / 2, 0, croppedWidth, img.getHeight()));
            } else {
                // L'image est plus haute que large (Portrait)
                double scale = displaySize / img.getWidth();
                double croppedHeight = displaySize / scale;
                imgMedecin.setViewport(new javafx.geometry.Rectangle2D(0, (img.getHeight() - croppedHeight) / 3, img.getWidth(), croppedHeight));
            }

        } catch (Exception e) {
            // Image de secours si erreur
            String encodedName = (nom + "+" + prenom).replace(" ", "+");
            imgMedecin.setImage(new Image("https://ui-avatars.com/api/?name=" + encodedName + "&background=EBF2FF&color=4178F5&size=150&bold=true"));
            imgMedecin.setViewport(null); // Pas de recadrage sur l'avatar par défaut
        }

        // --- MASQUE CIRCULAIRE (sur l'image recadrée) ---
        Circle clip = new Circle(displaySize / 2, displaySize / 2, displaySize / 2);
        imgMedecin.setClip(clip);

        // Charger les créneaux
        chargerDisponibilites();
    }

    private void chargerDisponibilites() {
        try {
            List<Disponibilite> list = disponibiliteService.findAvailableByMedecin(medecinId);
            
            if (lblCount != null) {
                lblCount.setText(list.size() + " créneaux disponibles trouvés");
            }

            gridDispo.getChildren().clear();

            for (Disponibilite d : list) {
                VBox card = createSlotCard(d);
                gridDispo.getChildren().add(card);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (lblCount != null) {
                lblCount.setText("Erreur lors du chargement des créneaux");
            }
        }
    }

    private VBox createSlotCard(Disponibilite d) {
        VBox card = new VBox(12); // Espacement légèrement augmenté
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 20; " +
                "-fx-border-color: #f1f5f9; -fx-border-radius: 20; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        card.setPrefWidth(220);

        // 1. Jour (ex: LUNDI)
        Label lblJour = new Label(d.getDateDebut().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase());
        lblJour.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        // 2. Date (ex: 12 AVR 2026)
        Label lblDate = new Label(d.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH)));
        lblDate.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 16; -fx-font-weight: bold;");

        // 3. Heure (Simple Label stylisé en badge)
        String timeStr = d.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + d.getDateFin().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-background-color: #f0f7ff; -fx-text-fill: #4178F5; -fx-padding: 5 12; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 13;");

        // 4. BOUTON RÉSERVER (Celui qui remplace l'ancien btnTime)
        Button btnReserver = new Button("Réserver");
        // Utilisation de votre classe CSS btn-primary-pill ou style inline
        btnReserver.setStyle("-fx-background-color: #4178F5; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 50; -fx-cursor: hand; -fx-padding: 8 25; -fx-font-size: 13;");
        btnReserver.setMaxWidth(Double.MAX_VALUE); // Prend toute la largeur de la carte

        // --- LOGIQUE DE NAVIGATION ---
        btnReserver.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FinaliserRdv.fxml"));
                Parent root = loader.load();

                // On passe les données à la page de finalisation
                FinaliserRdvController controller = loader.getController();
                
                com.hospismart.hospismartdesktop.models.User m = new com.hospismart.hospismartdesktop.models.User();
                m.setId(this.medecinId);
                String fullNom = lblNomMedecin.getText().replace("DR. ", "");
                m.setNom(fullNom); 
                d.setMedecin(m);
                controller.setDonneesRdv(d);

                // --- OUVERTURE EN MODAL ---
                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.initStyle(StageStyle.TRANSPARENT);
                
                Stage ownerStage = (Stage) btnReserver.getScene().getWindow();
                modalStage.initOwner(ownerStage);
                
                // On fait en sorte que la Scene de la modal fasse la taille de la fenêtre parente
                // pour que le fond gris (rgba(0,0,0,0.5)) couvre tout.
                Scene scene = new Scene(root, ownerStage.getWidth(), ownerStage.getHeight());
                scene.setFill(Color.TRANSPARENT);
                
                modalStage.setX(ownerStage.getX());
                modalStage.setY(ownerStage.getY());
                
                modalStage.setScene(scene);
                modalStage.showAndWait();

            } catch (IOException e) {
                System.err.println("Erreur chargement FinaliserRdv : " + e.getMessage());
                e.printStackTrace();
            }
        });

        // On ajoute tous les éléments à la VBox
        card.getChildren().addAll(lblJour, lblDate, lblTime, btnReserver);

        // Effet de survol sur la carte pour l'interactivité
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #4178F5; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle() + "-fx-border-color: #f1f5f9; -fx-border-width: 1;"));

        return card;
    }

    @FXML
    private void handleBackAction(javafx.event.ActionEvent event) {
        try {
            // 1. Charger le FXML de la liste
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/listeMedecinsParService.fxml"));
            Parent root = loader.load();

            // 2. Récupérer le contrôleur pour lui redonner les données du service
            MedecinController controller = loader.getController();

            // IMPORTANT : On utilise les variables qu'on a mémorisées dans setMedecinData
            controller.listerMedecins(this.currentServiceId, this.currentServiceName);

            // 3. Afficher la vue
            btnRetour.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Erreur lors du retour : " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    public void initialize() {
        // 1. Désactiver les styles par défaut moche de JavaFX pour Hyperlink
        btnRetour.setUnderline(false); // Pas de soulignement
        btnRetour.setStyle("-fx-text-fill: #4178F5; -fx-font-weight: bold; -fx-background-color: transparent;");

        // 2. Définir un style explicite pour le HOVER
        btnRetour.setOnMouseEntered(e -> {
            btnRetour.setStyle("-fx-text-fill: #2a5fcf; -fx-font-weight: bold; -fx-background-color: transparent;");
        });

        // 3. Revenir au style normal quand la souris part
        btnRetour.setOnMouseExited(e -> {
            btnRetour.setStyle("-fx-text-fill: #4178F5; -fx-font-weight: bold; -fx-background-color: transparent;");
        });

        // Configuration de l'action du clic (votre code existant)
        btnRetour.setOnAction(this::handleBackAction);
    }
}