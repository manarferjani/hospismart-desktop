package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.models.Inscription;
import com.hospismart.hospismartdesktop.services.EvenementService;
import com.hospismart.hospismartdesktop.services.InscriptionService;
import com.hospismart.hospismartdesktop.services.PdfExportService;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.layout.StackPane;

public class EvenementBackController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label            countLabel;
    @FXML private Label            footerLabel;

    @FXML private TableView<Evenement>         evenementTable;
    @FXML private TableColumn<Evenement, String>  colTitre;
    @FXML private TableColumn<Evenement, String>  colType;
    @FXML private TableColumn<Evenement, String>  colDateDebut;
    @FXML private TableColumn<Evenement, String>  colDateFin;
    @FXML private TableColumn<Evenement, String>  colLieu;
    @FXML private TableColumn<Evenement, String>  colStatut;
    @FXML private TableColumn<Evenement, Double>  colBudget;
    @FXML private TableColumn<Evenement, Void>    colActions;

    // ── Pagination ────────────────────────────────────────────
    @FXML private HBox paginationBox;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label pageInfoLabel;

    private final EvenementService         service = new EvenementService();
    private final ObservableList<Evenement> data    = FXCollections.observableArrayList();
    private static final DateTimeFormatter  FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Pagination state
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<Evenement> allData; // full result set

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupTable();
        loadData();
    }

    private void setupFilters() {
        typeFilter.getItems().addAll("Tous", "reunion", "formation", "visite", "maintenance", "autre");
        typeFilter.setValue("Tous");
        statutFilter.getItems().addAll("Tous", "planifie", "en_cours", "termine", "annule");
        statutFilter.setValue("Tous");
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeEvenement"));

        colDateDebut.setCellValueFactory(cd -> {
            Evenement e = cd.getValue();
            return new SimpleStringProperty(e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "-");
        });
        colDateFin.setCellValueFactory(cd -> {
            Evenement e = cd.getValue();
            return new SimpleStringProperty(e.getDateFin() != null ? e.getDateFin().format(FMT) : "-");
        });

        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colBudget.setCellValueFactory(new PropertyValueFactory<>("budgetAlloue"));

        // Type — styled badge matching .type-badge from web
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#475569; " +
                    "-fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                setGraphic(badge);
            }
        });

        // Statut — colored badges matching web .status-badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String style = switch (item) {
                    case "planifié", "planifie" ->
                        "-fx-background-color:#fef3c7; -fx-text-fill:#92400e;";
                    case "en_cours" ->
                        "-fx-background-color:#dbeafe; -fx-text-fill:#1e40af;";
                    case "terminé", "termine" ->
                        "-fx-background-color:#dcfce7; -fx-text-fill:#166534;";
                    case "annulé", "annule" ->
                        "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;";
                    default ->
                        "-fx-background-color:#f1f5f9; -fx-text-fill:#475569;";
                };
                badge.setStyle(style + " -fx-padding: 4 10; -fx-background-radius: 20; " +
                    "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-transform: uppercase;");
                setGraphic(badge);
            }
        });

        // Budget — formatted
        colBudget.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item > 0 ? String.format("%.0f TND", item) : "-");
                setStyle("-fx-font-weight:600; -fx-text-fill:#2c3e50;");
            }
        });

        // Actions — Voir + Modifier + Supprimer + PDF (Boutons modernes avec emojis/icônes)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn   = new Button("👁️");
            private final Button editBtn   = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button pdfBtn    = new Button("📄");
            private final HBox   box       = new HBox(8, viewBtn, editBtn, deleteBtn, pdfBtn);
            {
                viewBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                editBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4f46e5; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                pdfBtn.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                
                viewBtn.setTooltip(new Tooltip("Voir les détails"));
                editBtn.setTooltip(new Tooltip("Modifier"));
                deleteBtn.setTooltip(new Tooltip("Supprimer"));
                pdfBtn.setTooltip(new Tooltip("Exporter PDF"));
                
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
                pdfBtn.setOnAction(ev -> {
                    Evenement item = getTableView().getItems().get(getIndex());
                    if (item != null) exportPdf(item);
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
        allData = service.findAll();
        currentPage = 1;
        applyPagination();
    }

    private void loadFilteredData() {
        String kw     = searchField.getText();
        String type   = "Tous".equals(typeFilter.getValue())   ? "" : typeFilter.getValue();
        String statut = "Tous".equals(statutFilter.getValue()) ? "" : statutFilter.getValue();
        allData = service.search(kw, type, statut);
        currentPage = 1;
        applyPagination();
    }

    private void applyPagination() {
        int totalItems = allData.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        data.clear();
        if (fromIndex < totalItems) {
            data.addAll(allData.subList(fromIndex, toIndex));
        }

        refreshCount();
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " / " + totalPages
                    + "  (" + allData.size() + " résultats)");
        }
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages);
    }

    private void refreshCount() {
        if (countLabel != null)
            countLabel.setText(allData.size() + " evenement(s) enregistres.");
        if (footerLabel != null)
            footerLabel.setText("Affichage " + data.size() + " sur " + allData.size() + " evenements.");
    }

    // ── Pagination actions ────────────────────────────────────────────────────

    @FXML private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            applyPagination();
        }
    }

    @FXML private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            applyPagination();
        }
    }

    // ── Toolbar actions ───────────────────────────────────────────────────────

    @FXML private void onSearch() {
        loadFilteredData();
    }

    @FXML private void onReset() {
        searchField.clear();
        typeFilter.setValue("Tous");
        statutFilter.setValue("Tous");
        loadData();
    }

    @FXML private void onAdd() { openForm(null); }

    @FXML private void onRetour() {
        navigateToWelcome();
    }

    @FXML private void onGoFront(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-front.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 760);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) evenementTable.getScene().getWindow();
            stage.setTitle("HospiSmart — Espace Public");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void onGoStats() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-stats.fxml"));
            javafx.scene.Parent root = loader.load();
            StackPane contentArea = (StackPane) evenementTable.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/dashboard.fxml")); // Default to Dashboard if welcome isn't meant for contentArea
            javafx.scene.Parent root = loader.load();
            StackPane contentArea = (StackPane) evenementTable.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── CRUD helpers ──────────────────────────────────────────────────────────

    private void onDelete(Evenement e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'evenement ?");
        alert.setContentText("Voulez-vous vraiment supprimer \"" + e.getTitre() + "\" ?");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (service.delete(e.getId())) {
                    loadData();
                } else {
                    showError("Erreur", "Impossible de supprimer l'evenement.");
                }
            }
        });
    }

    private void showDetail(Evenement e) {
        String details = String.format(
                "Titre   : %s%nType    : %s%nStatut  : %s%nLieu    : %s%nDebut   : %s%nFin     : %s%nBudget  : %.0f TND%n%nDescription :%n%s",
                nvl(e.getTitre()), nvl(e.getTypeEvenement()), nvl(e.getStatut()), nvl(e.getLieu()),
                e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "-",
                e.getDateFin()   != null ? e.getDateFin().format(FMT)   : "-",
                e.getBudgetAlloue(),
                nvl(e.getDescription())
        );
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Detail - " + e.getTitre());
        a.setHeaderText(null);
        a.setContentText(details);
        a.showAndWait();
    }

    private void openForm(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-form.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load(), 750, 750);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(evenement == null ? "Ajouter un Evenement" : "Modifier - " + evenement.getTitre());
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

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── PDF Export ─────────────────────────────────────────────────────────────

    private void exportPdf(Evenement e) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le PDF de l'événement");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        
        String safeName = e.getTitre() != null ? e.getTitre().replaceAll("[^a-zA-Z0-9À-ÿ]", "_").replaceAll("_+", "_") : "Event";
        fileChooser.setInitialFileName("Evenement_" + safeName + "_" + e.getId() + ".pdf");
        
        java.io.File targetFile = fileChooser.showSaveDialog(evenementTable.getScene().getWindow());
        if (targetFile == null) return; // User cancelled

        PdfExportService pdfService = new PdfExportService();
        InscriptionService inscService = new InscriptionService();
        java.util.List<Inscription> participants = inscService.findByEvenement(e.getId());

        java.io.File file = pdfService.exportEvent(e, participants, targetFile);
        if (file != null && file.exists()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF Exporté");
            alert.setHeaderText(null);
            alert.setContentText("Le fichier PDF a été enregistré avec succès :\n\n" + file.getAbsolutePath());
            alert.showAndWait();

            // Open the file
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                System.err.println("Cannot open PDF: " + ex.getMessage());
            }
        } else {
            showError("Erreur", "Impossible de générer le fichier PDF.");
        }
    }
}
