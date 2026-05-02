package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MesRdvController {

    @FXML
    private FlowPane gridRdv;
    @FXML
    private VBox emptyState;
    @FXML
    private Label lblSubtitle;

    private final RendezVousService rdvService = new RendezVousService();

    @FXML
    public void initialize() {
        // Session de test si aucun utilisateur connecté
        if (UserSession.getUser() == null) {
            User userTest = new User();
            userTest.setId(1);
            userTest.setNom("Testeur");
            userTest.setPrenom("Patient");
            UserSession.login(userTest);
            System.out.println("DEBUG MesRdvController: Session de test activée, patient ID: 1");
        }

        refreshList();
    }

    private void refreshList() {
        int patientId = UserSession.getUser().getId();
        System.out.println("DEBUG MesRdvController: Chargement des RDV pour patient ID = " + patientId);

        try {
            List<RendezVous> listeBrute = rdvService.findByPatient(patientId);

            // On ne garde que les rendez-vous En attente ou Accepté
            List<RendezVous> list = listeBrute.stream()
                    .filter(r -> "En attente".equalsIgnoreCase(r.getStatut())
                            || "Accepté".equalsIgnoreCase(r.getStatut()))
                    .toList();

            System.out.println("DEBUG MesRdvController: " + list.size() + " rendez-vous filtrés à afficher");

            gridRdv.getChildren().clear();

            if (list.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                gridRdv.setVisible(false);
                gridRdv.setManaged(false);
                lblSubtitle.setText("Aucun rendez-vous trouvé (Session Patient ID: " + patientId + ").");
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                gridRdv.setVisible(true);
                gridRdv.setManaged(true);

                long enAttente = list.stream().filter(r -> "En attente".equals(r.getStatut())).count();
                long acceptes = list.stream().filter(r -> "Accepté".equals(r.getStatut())).count();
                lblSubtitle.setText(list.size() + " rendez-vous  \u2022  " + acceptes + " confirmé(s)  \u2022  "
                        + enAttente + " en attente");

                for (RendezVous rdv : list) {
                    System.out.println("DEBUG: RDV id=" + rdv.getId() + " statut=" + rdv.getStatut() + " medecin="
                            + rdv.getMedecinName());
                    gridRdv.getChildren().add(createRdvCard(rdv));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("ERREUR SQL MesRdvController: " + e.getMessage());
        }
    }

    private VBox createRdvCard(RendezVous rdv) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setPrefWidth(340);
        card.setMaxWidth(340);

        // Couleur de la bordure gauche selon le statut
        String borderColor;
        String statut = rdv.getStatut() != null ? rdv.getStatut() : "En attente";

        switch (statut) {
            case "Accepté":
                borderColor = "#198754";
                break;
            case "Refusé":
                borderColor = "#dc3545";
                break;
            default:
                borderColor = "#f59e0b";
                break;
        }

        String baseStyle = "-fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 4);";
        card.setStyle(baseStyle);

        // --- EN-TÊTE : Statut + Date ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblStatut = new Label(statut);
        String badgeStyle = "-fx-padding: 4 12; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 11;";

        switch (statut) {
            case "Accepté":
                lblStatut.setStyle(badgeStyle + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
                break;
            case "Refusé":
                lblStatut.setStyle(badgeStyle + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                break;
            default:
                lblStatut.setStyle(badgeStyle + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                break;
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        dateBox.setPadding(new Insets(5, 10, 5, 10));
        dateBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10;");

        Label lblHeure = new Label(rdv.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblHeure.setStyle("-fx-font-weight: 800; -fx-text-fill: #1a1a2e; -fx-font-size: 15;");

        Label lblDate = new Label(
                rdv.getDatetime().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH)).toUpperCase());
        lblDate.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(lblHeure, lblDate);
        header.getChildren().addAll(lblStatut, spacer, dateBox);

        // --- MÉDECIN ---
        VBox docInfo = new VBox(3);
        String medecinName = rdv.getMedecinName() != null ? rdv.getMedecinName() : "Dr. Médecin #" + rdv.getMedecinId();
        Label lblDoc = new Label(medecinName);
        lblDoc.setStyle("-fx-font-size: 17; -fx-font-weight: 700; -fx-text-fill: #1a1a2e;");

        String specialite = rdv.getSpecialite() != null ? rdv.getSpecialite() : "Spécialité Médicale";
        Label lblSpec = new Label(specialite);
        lblSpec.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        docInfo.getChildren().addAll(lblDoc, lblSpec);

        // --- MOTIF ---
        VBox motifBox = new VBox(4);
        motifBox.setPadding(new Insets(10));
        motifBox.setStyle(
                "-fx-background-color: #fafbfc; -fx-border-color: #e2e8f0; " +
                        "-fx-border-style: dashed; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblMotifTitle = new Label("Motif de consultation");
        lblMotifTitle.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        String motifText = rdv.getMotif() != null ? rdv.getMotif() : "Non précisé";
        Label lblMotifText = new Label("\"" + motifText + "\"");
        lblMotifText.setStyle("-fx-font-style: italic; -fx-text-fill: #4a5568; -fx-font-size: 13;");
        lblMotifText.setWrapText(true);

        motifBox.getChildren().addAll(lblMotifTitle, lblMotifText);

        // --- BOUTON ANNULER ---
        Button btnAnnuler = new Button("Annuler le rendez-vous");
        btnAnnuler.setMaxWidth(Double.MAX_VALUE);
        String btnNormalStyle = "-fx-background-color: transparent; -fx-border-color: #dc3545; -fx-border-radius: 10; "
                +
                "-fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 12;";
        btnAnnuler.setStyle(btnNormalStyle);

        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
                "-fx-background-color: #fee2e2; -fx-border-color: #dc3545; -fx-border-radius: 10; " +
                        "-fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 12;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(btnNormalStyle));

        if ("Refusé".equals(statut)) {
            btnAnnuler.setText("Rendez-vous refusé");
            btnAnnuler.setDisable(true);
            btnAnnuler.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-border-color: transparent; -fx-background-radius: 10; -fx-padding: 10; -fx-font-size: 12;");
        }

        btnAnnuler.setOnAction(e -> handleAnnuler(rdv));

        card.getChildren().addAll(header, docInfo, motifBox, btnAnnuler);

        // Hover sur la carte
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 20, 0, 0, 6);";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    private void handleAnnuler(RendezVous rdv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment annuler ce rendez-vous ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmer l'annulation");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    rdvService.deleteOne(rdv.getId());
                    refreshList();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleNouveauRdv() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TrouverMedecin.fxml"));
            Parent root = loader.load();
            gridRdv.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}