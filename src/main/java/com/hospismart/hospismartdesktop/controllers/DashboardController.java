package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML private Label lblSidebarName, lblWelcome, lblDate;
    @FXML private StackPane contentArea;
    @FXML private Label statRdvJour, statAttente, statPatients;
    @FXML private Label lblNextPatientName, lblNextPatientTime, lblNextPatientMotif;
    @FXML private ImageView imgNextPatient;
    @FXML private TableView<RendezVous> tableRdv;
    @FXML private TableColumn<RendezVous, String> colPatient, colDate, colStatut;
    @FXML private TableColumn<RendezVous, Void> colAction;

    public StackPane getContentArea() { return contentArea; }


    private final RendezVousService rdvService = new RendezVousService();
    private RendezVous currentNextRdv;
    private boolean isNavigating = false; // Sécurité anti-boucle

    @FXML
    public void initialize() {
        // 1. Initialisation Session (Uniquement si vide)
        if (UserSession.getUser() == null) {
            User user = new User();
            user.setId(4);
            user.setNom("Ferjani");
            user.setPrenom("Nour");
            UserSession.login(user);
        }

        // 2. Sidebar (Éléments statiques)
        if (lblSidebarName != null) {
            lblSidebarName.setText("Dr. " + UserSession.getUser().getPrenom() + " " + UserSession.getUser().getNom());
        }

        // 3. Charger l'accueil par défaut au premier lancement
        // On vérifie si contentArea est vide pour ne pas recharger si on est déjà dedans
        Platform.runLater(() -> {
            if (contentArea != null && contentArea.getChildren().isEmpty()) {
                loadPage("HomeDashboard.fxml");
            }
        });
    }

    private void loadPage(String fxmlFileName) {
        if (isNavigating) return; // Bloque si une navigation est déjà en cours

        try {
            isNavigating = true;
            System.out.println("🔄 Tentative de navigation vers : " + fxmlFileName);

            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/views/" + fxmlFileName);
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/" + fxmlFileName);
            if (fxmlLocation == null) throw new IOException("FXML introuvable : " + fxmlFileName);

            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // On ne force le contrôleur QUE pour le HomeDashboard
            if (fxmlFileName.equals("HomeDashboard.fxml")) {
                loader.setController(this);
            }

            Parent root = loader.load();
            contentArea.getChildren().setAll(root);

            if (fxmlFileName.equals("HomeDashboard.fxml")) {
                refreshDashboardData();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            isNavigating = false; // Libère le verrou
        }
    }

    private void refreshDashboardData() {
        if (lblWelcome == null) return; // Sécurité si le FXML n'est pas bien chargé

        lblWelcome.setText("Bienvenue, Dr. " + UserSession.getUser().getPrenom());
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        try {
            int medecinId = UserSession.getUser().getId();

            // 1. Prochain Patient
            RendezVous nextRdv = rdvService.findNextByMedecin(medecinId);
            this.currentNextRdv = nextRdv;
            if (nextRdv != null) {
                lblNextPatientName.setText(nextRdv.getPatientName());
                lblNextPatientTime.setText(nextRdv.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm")));
                lblNextPatientMotif.setText(nextRdv.getMotif());
            } else {
                lblNextPatientName.setText("Aucun patient");
                lblNextPatientTime.setText("--:--");
                lblNextPatientMotif.setText("Aucun RDV accepté");
            }

            // 2. Stats (Dynamique pour RDV du jour et En Attente)
            List<RendezVous> allRdv = rdvService.findALL(); // Idéalement, créer des méthodes count directes
            long rdvJour = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && r.getDatetime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
            long enAttente = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && "En attente".equals(r.getStatut()))
                .count();

            statRdvJour.setText(String.valueOf(rdvJour));
            statAttente.setText(String.valueOf(enAttente));
            statPatients.setText("150"); // Placeholder pour le moment

        } catch (Exception e) {
            System.err.println("Erreur chargement Dashboard : " + e.getMessage());
        }

        setupTable();
    }

    private void setupTable() {
        if (tableRdv == null) return;

        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDatetime().format(DateTimeFormatter.ofPattern("HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnAccept = new Button("✅");
            private final Button btnRefuse = new Button("❌");
            private final HBox container = new HBox(8, btnAccept, btnRefuse);

            {
                btnAccept.getStyleClass().add("btn-action-accept");
                btnRefuse.getStyleClass().add("btn-action-refuse");

                btnAccept.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Accepté");
                });

                btnRefuse.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Annulé");
                });
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        try {
            int medecinId = UserSession.getUser().getId();
            List<RendezVous> data = rdvService.findPendingByMedecin(medecinId);
            tableRdv.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) {
            System.err.println("Erreur SQL Table : " + e.getMessage());
        }
    }

    @FXML public void showDashboard(ActionEvent e) { loadPage("HomeDashboard.fxml"); }
    @FXML public void showDemandesRdv(ActionEvent e) { loadPage("mesDemandesRDV.fxml"); }
    @FXML public void showDisponibilites(ActionEvent e) { loadPage("MedecinDisponibilites.fxml"); }
    @FXML public void showMesPatients(ActionEvent e) { loadPage("MesPatients.fxml"); }

    @FXML
    public void handleStartConsultation(ActionEvent event) {
        if (currentNextRdv == null) {
            System.err.println("Aucun patient à consulter");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/views/NewConsultation.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/NewConsultation.fxml");
            
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            
            NewConsultationController controller = loader.getController();
            controller.setRendezVous(currentNextRdv);
            controller.setDashboardController(this);
            
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            System.err.println("Erreur chargement NewConsultation.fxml : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUpdateStatut(int rdvId, String nouveauStatut) {
        try {
            rdvService.updateStatut(rdvId, nouveauStatut);
            refreshDashboardData(); // Rafraîchir pour enlever le RDV traité
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
        }
    }
}