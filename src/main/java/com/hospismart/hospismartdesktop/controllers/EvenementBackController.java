package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.services.EvenementService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EvenementBackController implements Initializable {

    // ── Toolbar ──────────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label            countLabel;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<Evenement>         evenementTable;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String>  colTitre;
    @FXML private TableColumn<Evenement, String>  colType;
    @FXML private TableColumn<Evenement, String>  colDateDebut;
    @FXML private TableColumn<Evenement, String>  colDateFin;
    @FXML private TableColumn<Evenement, String>  colLieu;
    @FXML private TableColumn<Evenement, String>  colStatut;
    @FXML private TableColumn<Evenement, Double>  colBudget;
    @FXML private TableColumn<Evenement, Void>    colActions;

    private final EvenementService         service = new EvenementService();
    private final ObservableList<Evenement> data    = FXCollections.observableArrayList();
    private static final DateTimeFormatter  FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupTable();
        loadData();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupFilters() {
        typeFilter.getItems().addAll("Tous", "réunion", "formation", "visite", "maintenance", "autre");
        typeFilter.setValue("Tous");
        statutFilter.getItems().addAll("Tous", "planifié", "en_cours", "terminé", "annulé");
        statutFilter.setValue("Tous");
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {

        // ── Value factories ────────────────────────────────────────────
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeEvenement"));

        colDateDebut.setCellValueFactory(cd -> {
            Evenement e = cd.getValue();
            return new SimpleStringProperty(e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "—");
        });
        colDateFin.setCellValueFactory(cd -> {
            Evenement e = cd.getValue();
            return new SimpleStringProperty(e.getDateFin() != null ? e.getDateFin().format(FMT) : "—");
        });

        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colBudget.setCellValueFactory(new PropertyValueFactory<>("budgetAlloue"));

        // ── Cell factories ────────────────────────────────────────────

        // ID — muted grey text
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText("#" + item);
                setStyle("-fx-text-fill:#7f8c8d; -fx-font-size:12px;");
            }
        });

        // Type — grey pill badge matching web .type-badge
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#475569;" +
                        "-fx-padding:3 8; -fx-background-radius:6; -fx-font-size:11px; -fx-font-weight:600;");
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                badge.setText(item);
                setGraphic(badge);
            }
        });

        // Statut — coloured pill badge matching web .status-badge colours
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { setAlignment(Pos.CENTER_LEFT); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                badge.setText(item.toUpperCase().replace("_", " "));
                String style = switch (item) {
                    case "planifié"  -> "-fx-background-color:#fef3c7; -fx-text-fill:#92400e;";
                    case "en_cours"  -> "-fx-background-color:#dbeafe; -fx-text-fill:#1e40af;";
                    case "terminé"   -> "-fx-background-color:#dcfce7; -fx-text-fill:#166534;";
                    case "annulé"    -> "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;";
                    default          -> "-fx-background-color:#ecf0f1; -fx-text-fill:#2c3e50;";
                };
                badge.setStyle(style +
                        "-fx-padding:4 12; -fx-background-radius:20;" +
                        "-fx-font-size:10px; -fx-font-weight:700;");
                setGraphic(badge);
            }
        });

        // Budget — formatted
        colBudget.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item > 0 ? String.format("%.0f TND", item) : "—");
                setStyle("-fx-font-weight:700;");
            }
        });

        // Actions — three web-style light buttons with text labels
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn   = new Button("Voir");
            private final Button editBtn   = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox   box       = new HBox(6, viewBtn, editBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("btn-view");
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-delete");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(ev -> {
                    Evenement item = getTableView().getItems().get(getIndex());
                    if (item != null) openForm(item);
                });
                deleteBtn.setOnAction(ev -> {
                    Evenement item = getTableView().getItems().get(getIndex());
                    if (item != null) onDelete(item);
                });
                viewBtn.setOnAction(ev -> {
                    Evenement item = getTableView().getItems().get(getIndex());
                    if (item != null) showDetail(item);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        evenementTable.setItems(data);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void loadData() {
        data.clear();
        data.addAll(service.findAll());
        refreshCount();
    }

    private void refreshCount() {
        if (countLabel != null)
            countLabel.setText(data.size() + " événement(s)");
    }

    // ── Toolbar actions ───────────────────────────────────────────────────────

    @FXML private void onSearch() {
        String kw     = searchField.getText();
        String type   = "Tous".equals(typeFilter.getValue())   ? "" : typeFilter.getValue();
        String statut = "Tous".equals(statutFilter.getValue()) ? "" : statutFilter.getValue();
        data.clear();
        data.addAll(service.search(kw, type, statut));
        refreshCount();
    }

    @FXML private void onReset() {
        searchField.clear();
        typeFilter.setValue("Tous");
        statutFilter.setValue("Tous");
        loadData();
    }

    @FXML private void onAdd() { openForm(null); }

    /** Navigate back to the welcome screen. */
    @FXML private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/welcome-view.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());
            Stage stage = (Stage) evenementTable.getScene().getWindow();
            stage.setTitle("HospiSmart — Bienvenue");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── CRUD helpers ──────────────────────────────────────────────────────────

    private void onDelete(Evenement e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'événement ?");
        alert.setContentText("Voulez-vous vraiment supprimer \"" + e.getTitre() + "\" ?\nCette action est irréversible.");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (service.delete(e.getId())) {
                    loadData();
                } else {
                    showError("Erreur", "Impossible de supprimer l'événement.");
                }
            }
        });
    }

    private void showDetail(Evenement e) {
        String details = String.format(
                "Titre   : %s%nType    : %s%nStatut  : %s%nLieu    : %s%nDébut   : %s%nFin     : %s%nBudget  : %.0f TND%n%nDescription :%n%s",
                nvl(e.getTitre()), nvl(e.getTypeEvenement()), nvl(e.getStatut()), nvl(e.getLieu()),
                e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "—",
                e.getDateFin()   != null ? e.getDateFin().format(FMT)   : "—",
                e.getBudgetAlloue(),
                nvl(e.getDescription())
        );
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Détail — " + e.getTitre());
        a.setHeaderText(null);
        a.setContentText(details);
        a.showAndWait();
    }

    private void openForm(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-form.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load(), 700, 640);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(evenement == null ? "Ajouter un Événement" : "Modifier — " + evenement.getTitre());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            EvenementFormController fc = loader.getController();
            fc.setEvenement(evenement);

            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire :\n" + e.getMessage());
        }
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
