package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Notification;
import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.services.AIService;
import com.hospismart.hospismartdesktop.services.NotificationService;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DemandesRdvController {

    @FXML
    private TableView<RendezVous> tableDemandes;
    @FXML
    private TableColumn<RendezVous, String> colDate;
    @FXML
    private TableColumn<RendezVous, String> colHeure;
    @FXML
    private TableColumn<RendezVous, String> colPatient;
    @FXML
    private TableColumn<RendezVous, String> colMotif;
    @FXML
    private TableColumn<RendezVous, Number> colPriorite;
    @FXML
    private TableColumn<RendezVous, Void> colActions;
    @FXML
    private Label lblCount;
    @FXML
    private VBox emptyState;

    private final RendezVousService rdvService = new RendezVousService();
    private final NotificationService notifService = new NotificationService();
    private final AIService aiService;
    private final ObservableList<RendezVous> masterData = FXCollections.observableArrayList();

    public DemandesRdvController() {
        // Récupération de la clé API Gemini depuis les variables d'environnement
        String geminiKey = System.getenv("GEMINI_API_KEY");
        this.aiService = new AIService(geminiKey != null ? geminiKey : "");
    }

    @FXML
    public void initialize() {
        setupColumns();
        refreshData();
    }

    private void setupColumns() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        colDate.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getDatetime().format(dateFormatter)));

        colHeure.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getDatetime().format(timeFormatter)));

        colPatient.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getPatientName();
            return new SimpleStringProperty(
                    (name != null && !name.isBlank()) ? name : "Patient #" + cellData.getValue().getPatientId());
        });

        colMotif.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMotif()));

        // Custom cell factory: truncate long motif text + tooltip on hover
        colMotif.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String motif, boolean empty) {
                super.updateItem(motif, empty);
                if (empty || motif == null || motif.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(motif);
                    setStyle("-fx-text-overrun: ellipsis;");
                    setMaxWidth(Control.USE_PREF_SIZE);
                    Tooltip tip = new Tooltip(motif);
                    tip.setWrapText(true);
                    tip.setMaxWidth(350);
                    setTooltip(tip);
                }
            }
        });

        setupPrioriteColumn();
        setupActionsColumn();
    }

    /**
     * Configure la colonne Priorité avec des badges visuels colorés.
     * L'IA analyse le motif et retourne un score de 1 à 5.
     */
    private void setupPrioriteColumn() {
        colPriorite.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getPriorite()));

        colPriorite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number priorite, boolean empty) {
                super.updateItem(priorite, empty);
                if (empty || priorite == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int p = priorite.intValue();
                    Label badge = new Label(getPrioriteLabel(p));
                    badge.setStyle(getPrioriteBadgeStyle(p));
                    badge.setAlignment(Pos.CENTER);
                    badge.setMinWidth(100);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private String getPrioriteLabel(int priorite) {
        return switch (priorite) {
            case 5 -> "🔴 CRITIQUE";
            case 4 -> "🟠 URGENT";
            case 3 -> "🟡 MOYEN";
            case 2 -> "🔵 STANDARD";
            case 1 -> "🟢 NON-URGENT";
            default -> "⚪ INCONNU";
        };
    }

    private String getPrioriteBadgeStyle(int priorite) {
        String bgColor = switch (priorite) {
            case 5 -> "#fde8e8";
            case 4 -> "#fff3e0";
            case 3 -> "#fff9c4";
            case 2 -> "#e3f2fd";
            case 1 -> "#e8f5e9";
            default -> "#f5f5f5";
        };
        String textColor = switch (priorite) {
            case 5 -> "#c62828";
            case 4 -> "#e65100";
            case 3 -> "#f57f17";
            case 2 -> "#1565c0";
            case 1 -> "#2e7d32";
            default -> "#616161";
        };
        return "-fx-background-color: " + bgColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 11; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 20;";
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnAccept = new Button("✅");
            private final Button btnRefuse = new Button("❌");
            private final HBox container = new HBox(8, btnAccept, btnRefuse);

            {
                btnAccept.getStyleClass().add("btn-action-accept");
                btnRefuse.getStyleClass().add("btn-action-refuse");

                // Alignement horizontal et vertical au centre de la cellule
                container.setAlignment(Pos.CENTER);

                btnAccept.setOnAction(event -> handleAccepter(getTableRow().getItem()));
                btnRefuse.setOnAction(event -> handleRefuser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null)
                        ? null
                        : container);
            }
        });
    }

    private void refreshData() {
        if (UserSession.getUser() == null) {
            System.out.println("⚠ DemandesRdvController : Aucun utilisateur connecté.");
            return;
        }

        try {
            int medecinId = UserSession.getUser().getId();
            List<RendezVous> pendingRdv = rdvService.findPendingByMedecin(medecinId);

            // Calculer la priorité IA pour chaque rendez-vous (toujours recalculer)
            for (RendezVous rdv : pendingRdv) {
                int prioriteIA = aiService.calculerPriorite(rdv.getMotif());
                rdv.setPriorite(prioriteIA);
            }

            masterData.setAll(pendingRdv);
            tableDemandes.setItems(masterData);

            int count = masterData.size();
            lblCount.setText(count == 0
                    ? "Aucune demande en attente."
                    : count + " demande(s) en attente de votre réponse.");

            boolean isEmpty = masterData.isEmpty();
            tableDemandes.setVisible(!isEmpty);
            tableDemandes.setManaged(!isEmpty);
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les demandes : " + e.getMessage());
        }
    }

    private void handleAccepter(RendezVous rdv) {
        if (rdv == null) return;
        showModernConfirmModal("Accepter le RDV", "Confirmez-vous l'acceptation du rendez-vous de " + rdv.getPatientName() + " ?", false, () -> {
            try {
                rdvService.updateStatut(rdv.getId(), "Accepté");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
                Notification notif = new Notification();
                notif.setContent("✅ Votre rendez-vous avec Dr. " + UserSession.getUser().getNom() + " le " + rdv.getDatetime().format(formatter) + " a été accepté !");
                notif.setUserId(rdv.getPatientId());
                notif.setType("rdv_accepte");
                notifService.insertOne(notif);
                refreshData();
                //showModernModal("Succès", "Rendez-vous accepté avec succès.", false);
            } catch (SQLException e) {
                showModernModal("Erreur", "Action impossible : " + e.getMessage(), true);
            }
        });
    }

    private void handleRefuser(RendezVous rdv) {
        if (rdv == null) return;
        showModernConfirmModal("Refuser le RDV", "Êtes-vous sûr de vouloir refuser le rendez-vous de " + rdv.getPatientName() + " ?", true, () -> {
            try {
                rdvService.updateStatut(rdv.getId(), "Refusé");
                Notification notif = new Notification();
                notif.setContent("❌ Votre demande de rendez-vous avec Dr. " + UserSession.getUser().getNom() + " a été refusée.");
                notif.setUserId(rdv.getPatientId());
                notif.setType("rdv_refuse");
                notifService.insertOne(notif);
                refreshData();
                //showModernModal("Terminé", "Le rendez-vous a été refusé.", false);
            } catch (SQLException e) {
                showModernModal("Erreur", "Action impossible : " + e.getMessage(), true);
            }
        });
    }

    /**
     * Affiche la modal avec DEUX boutons (Confirm/Cancel)
     */
    private void showModernConfirmModal(String title, String message, boolean isError, Runnable onConfirm) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModernModal.fxml"));
            Parent root = loader.load();
            ModernModalController controller = loader.getController();
            controller.setData(title, message, isError, onConfirm);
            if (onConfirm != null) controller.showConfirmMode();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Window owner = tableDemandes.getScene().getWindow();
            stage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setX(owner.getX()); stage.setY(owner.getY());
            stage.setWidth(owner.getWidth()); stage.setHeight(owner.getHeight());
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showModernModal(String title, String message, boolean isError) {
        showModernConfirmModal(title, message, isError, null);
    }

    @FXML
    private void handleGoToDispos() {
        try {
            // Note: Remplacer le root de la scène supprime la Sidebar.
            // Pour rester dans le Dashboard, il faudrait passer par
            // DashboardController.loadPage()
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MedecinDisponibilites.fxml"));
            Parent root = loader.load();

            // On essaie de remonter au DashboardController si possible, sinon on change le
            // root
            if (tableDemandes.getScene().getRoot().lookup("#contentArea") instanceof StackPane area) {
                area.getChildren().setAll(root);
            } else {
                tableDemandes.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
