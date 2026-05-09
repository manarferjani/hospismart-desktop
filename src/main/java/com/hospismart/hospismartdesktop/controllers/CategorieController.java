package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Categorie;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class CategorieController {

    @FXML private TableView<Categorie> categorieTable;
    @FXML private TableColumn<Categorie, String> colNom;
    @FXML private TableColumn<Categorie, String> colDescription;
    @FXML private Label lblStatus;
    @FXML private Label lblStatTotal;
    @FXML private TextField txtSearch;

    private CategorieDAO categorieDAO = new CategorieDAO();
    private ObservableList<Categorie> categorieList = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Categorie> filteredList;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        filteredList = new javafx.collections.transformation.FilteredList<>(categorieList, p -> true);
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        // Colonne Actions
        TableColumn<Categorie, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(120);
        colActions.setResizable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4f46e5; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
                
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                box.setAlignment(javafx.geometry.Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    Categorie c = getTableRow().getItem();
                    if (c == null) return;
                    showDialog(c);
                });
                btnDelete.setOnAction(e -> {
                    Categorie c = getTableRow().getItem();
                    if (c == null) return;
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer « " + c.getNom() + " » ?", ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            categorieDAO.delete(c.getId());
                            handleRefresh();
                            lblStatus.setText("✅ « " + c.getNom() + " » supprimée.");
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        categorieTable.getColumns().add(colActions);

        handleRefresh();
    }

    @FXML
    public void handleShowAddDialog() { showDialog(null); }

    private void showDialog(Categorie existing) {
        boolean isEdit = (existing != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "✎  Modifier la catégorie" : "➕  Nouvelle catégorie");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType(isEdit ? "Enregistrer" : "Ajouter",
                                            ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:#f8fafc;-fx-font-size:13px;");
        dialog.getDialogPane().setPrefWidth(420);

        // ── Champs ────────────────────────────────────────────────────
        TextField fNom  = new TextField(isEdit ? existing.getNom() : "");
        TextField fDesc = new TextField(isEdit ? existing.getDescription() : "");

        String fs = "-fx-background-color:#f1f5f9;-fx-border-color:#cbd5e1;" +
                    "-fx-border-radius:7;-fx-background-radius:7;-fx-padding:6 10;";
        fNom.setStyle(fs);  fNom.setPromptText("Nom de la catégorie (obligatoire, unique)");
        fDesc.setStyle(fs); fDesc.setPromptText("Description (optionnel)");
        fNom.setPrefWidth(240); fDesc.setPrefWidth(240);

        // ── Label d'erreur inline ──────────────────────────────────────
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#dc3545;-fx-font-weight:bold;-fx-font-size:12px;-fx-wrap-text:true;");
        lblErr.setMaxWidth(380);
        lblErr.setMinHeight(28);

        // ── Grid ──────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(styledLabel("Nom *"),       0, 0); grid.add(fNom,  1, 0);
        grid.add(styledLabel("Description"), 0, 1); grid.add(fDesc, 1, 1);
        grid.add(lblErr,                     0, 2); GridPane.setColumnSpan(lblErr, 2);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.setStyle("-fx-background-color:#198754;-fx-text-fill:white;" +
                          "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:7 18;");

        // ── Validation bloquante ────────────────────────────────────────
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            StringBuilder sb = new StringBuilder();
            String nom = fNom.getText().trim();

            if (nom.isEmpty())
                sb.append("• Le nom est obligatoire.\n");
            else if (nom.length() < 2)
                sb.append("• Le nom doit contenir au moins 2 caractères.\n");
            else if (categorieDAO.existsByNom(nom, isEdit ? existing.getId() : 0))
                sb.append("• Une catégorie avec ce nom existe déjà.\n");

            if (!sb.isEmpty()) {
                lblErr.setText(sb.toString().trim());
                if (nom.isEmpty())
                    fNom.setStyle("-fx-border-color:#dc3545;-fx-border-width:2;" +
                                  "-fx-border-radius:7;-fx-background-radius:7;" +
                                  "-fx-background-color:#fff5f5;-fx-padding:6 10;");
                event.consume();
            }
        });

        fNom.textProperty().addListener((o,a,b) -> {
            lblErr.setText("");
            fNom.setStyle(fs);
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            Categorie c = isEdit ? existing : new Categorie();
            c.setNom(fNom.getText().trim());
            c.setDescription(fDesc.getText().trim());

            boolean ok = isEdit ? categorieDAO.update(c) : categorieDAO.add(c);
            handleRefresh();
            lblStatus.setText(ok
                ? (isEdit ? "✅ Catégorie modifiée !" : "✅ Catégorie ajoutée !")
                : "❌ Erreur lors de l'opération.");
        }
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#475569;-fx-font-weight:bold;");
        return l;
    }

    private void applyFilters() {
        if (filteredList == null) return;
        String search = txtSearch != null ? txtSearch.getText().toLowerCase() : "";
        filteredList.setPredicate(c ->
            search.isEmpty() ||
            c.getNom().toLowerCase().contains(search) ||
            (c.getDescription() != null && c.getDescription().toLowerCase().contains(search)));

        // Wrapper pour le tri cliquant sur les colonnes
        javafx.collections.transformation.SortedList<Categorie> sortedData = new javafx.collections.transformation.SortedList<>(filteredList);
        sortedData.comparatorProperty().bind(categorieTable.comparatorProperty());
        categorieTable.setItems(sortedData);
    }

    @FXML
    private void handleRefresh() {
        categorieList.setAll(categorieDAO.findAll());
        applyFilters();
        if (lblStatTotal != null) lblStatTotal.setText(String.valueOf(categorieList.size()));
        lblStatus.setText("✅ " + categorieList.size() + " catégories chargées.");
    }

    @FXML private void handleAdd()    { showDialog(null); }
    @FXML private void handleUpdate() { }
    @FXML private void handleDelete() { }
    @FXML private void clearForm()    { }
}
