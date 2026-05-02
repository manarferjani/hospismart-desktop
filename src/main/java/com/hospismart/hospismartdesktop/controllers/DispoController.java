package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Disponibilite;
import com.hospismart.hospismartdesktop.services.DisponibiliteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import com.hospismart.hospismartdesktop.utils.UserSession;
import java.sql.SQLException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;

public class DispoController {

    @FXML
    private TableView<Disponibilite> tableDispos;
    @FXML
    private TableColumn<Disponibilite, String> colJour, colDate, colHeure, colStatut;
    @FXML
    private TableColumn<Disponibilite, Void> colActions;
    @FXML
    private HBox alertBox;
    @FXML
    private Label lblAlertMessage;

    private final DisponibiliteService service = new DisponibiliteService();
    private ObservableList<Disponibilite> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        // 1. Colonne Jour (ex: Lundi)
        colJour.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateDebut()
                .getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH)));

        colJour.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String dayName = item.substring(0, 1).toUpperCase() + item.substring(1).toLowerCase();
                    Label badge = new Label(dayName);
                    badge.getStyleClass().add("badge-jour");
                    switch (dayName.toLowerCase()) {
                        case "lundi":
                            badge.getStyleClass().add("badge-lundi");
                            break;
                        case "mardi":
                            badge.getStyleClass().add("badge-mardi");
                            break;
                        case "mercredi":
                            badge.getStyleClass().add("badge-mercredi");
                            break;
                        case "jeudi":
                            badge.getStyleClass().add("badge-jeudi");
                            break;
                        case "vendredi":
                            badge.getStyleClass().add("badge-vendredi");
                            break;
                        case "samedi":
                            badge.getStyleClass().add("badge-samedi");
                            break;
                        case "dimanche":
                            badge.getStyleClass().add("badge-dimanche");
                            break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // 2. Colonne Date (ex: 12/04/2026)
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        // 3. Colonne Heure (ex: 08:00 - 10:00)
        colHeure.setCellValueFactory(cellData -> {
            String debut = cellData.getValue().getDateDebut()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String fin = cellData.getValue().getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            return new SimpleStringProperty(debut + " - " + fin);
        });

        // 4. Colonne Statut avec Badge
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Disponibilite d = getTableRow().getItem();
                    Label badge = new Label(d.isReserve() ? "Réservé" : "Libre");
                    badge.getStyleClass().add(d.isReserve() ? "badge-reserve" : "badge-libre");
                    setGraphic(badge);
                }
            }
        });

        // 5. Colonne Actions (Boutons Modifier/Supprimer)
        setupActionsColumn();
    }

    private void loadData() {
        try {
            // On ne charge que les disponibilités du médecin connecté
            int medecinId = UserSession.getUser().getId();
            masterData.setAll(service.findAvailableByMedecin(medecinId));
            tableDispos.setItems(masterData);

            // Simulation de l'alerte (à lier avec un service de demandes plus tard)
            long enAttente = 3;
            if (enAttente > 0) {
                lblAlertMessage.setText(enAttente + " demande(s) en attente de validation.");
                alertBox.setVisible(true);
            }
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de récupérer les disponibilités : " + e.getMessage());
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                Region editIcon = new Region();
                editIcon.getStyleClass().add("icon-edit");
                btnEdit.setGraphic(editIcon);
                btnEdit.getStyleClass().add("btn-outline-primary");

                Region deleteIcon = new Region();
                deleteIcon.getStyleClass().add("icon-delete");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.getStyleClass().add("btn-outline-danger");

                btnDelete.setOnAction(event -> {
                    Disponibilite d = getTableRow().getItem();
                    handleDelete(d);
                });

                btnEdit.setOnAction(event -> {
                    Disponibilite d = getTableRow().getItem();
                    handleEdit(d);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Disponibilite d = getTableRow().getItem();
                    // On empêche la suppression si c'est réservé (comme dans ton Twig)
                    btnDelete.setDisable(d.isReserve());
                    setGraphic(container);
                }
            }
        });
    }

    private void handleDelete(Disponibilite d) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce créneau ?", ButtonType.YES,
                ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.deleteOne(d.getId());
                    loadData();
                } catch (SQLException e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private void handleEdit(Disponibilite d) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddDispoModal.fxml"));
            Parent root = loader.load();

            AddDispoModalController controller = loader.getController();
            controller.setData(d);

            Stage stage = new Stage();
            stage.setTitle("Modifier le créneau");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur d'édition", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    private void handleNewCreneau() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddDispoModal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nouveau Créneau");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            AddDispoModalController controller = loader.getController();
            if (controller.isSaved()) {
                loadData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur d'interface", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}