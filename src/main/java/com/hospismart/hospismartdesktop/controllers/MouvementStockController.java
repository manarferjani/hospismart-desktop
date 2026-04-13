package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.services.MouvementStockDAO;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.models.Medicament;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MouvementStockController {

    @FXML private TableView<MouvementStock> mouvementTable;
    @FXML private TableColumn<MouvementStock, String>        colType;
    @FXML private TableColumn<MouvementStock, Integer>       colQuantite;
    @FXML private TableColumn<MouvementStock, LocalDateTime> colDate;
    @FXML private TableColumn<MouvementStock, String>        colCommentaire;
    @FXML private TableColumn<MouvementStock, String>        colMedicament;

    @FXML private Label lblStatus;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatEntrees;
    @FXML private Label lblStatSorties;
    @FXML private ComboBox<String> cbFilterType;
    @FXML private TextField txtSearch;

    private MouvementStockDAO mouvementDAO  = new MouvementStockDAO();
    private MedicamentDAO     medicamentDAO = new MedicamentDAO();
    private ObservableList<MouvementStock> mouvementList = FXCollections.observableArrayList();
    private FilteredList<MouvementStock>   filteredList;

    @FXML
    public void initialize() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateMouvement"));
        colCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        colMedicament.setCellValueFactory(new PropertyValueFactory<>("medicamentNom"));

        // Colorer ENTREE en vert, SORTIE en rouge
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("ENTREE".equals(item)
                        ? "-fx-text-fill:#198754;-fx-font-weight:bold;"
                        : "-fx-text-fill:#dc3545;-fx-font-weight:bold;");
                }
            }
        });

        // Colonne Actions (Supprimer uniquement)
        TableColumn<MouvementStock, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(120);
        colActions.setResizable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("✕  Supprimer");
            {
                btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;");
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));
                btnDelete.setOnMouseExited (e -> btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));

                btnDelete.setOnAction(e -> {
                    MouvementStock ms = getTableRow().getItem();
                    if (ms == null) return;
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer ce mouvement (" + ms.getType() + " de " + ms.getQuantite() + ") ?",
                        ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            mouvementDAO.delete(ms.getId());
                            handleRefresh();
                            lblStatus.setText("✅ Mouvement supprimé.");
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
        mouvementTable.getColumns().add(colActions);

        // Filtre type (ENTREE / SORTIE / Tous)
        filteredList = new FilteredList<>(mouvementList, p -> true);
        if (cbFilterType != null) {
            cbFilterType.setItems(FXCollections.observableArrayList(
                "Tous", "ENTREE", "SORTIE"));
            cbFilterType.getSelectionModel().selectFirst();
            cbFilterType.setOnAction(e -> applyFilters());
        }
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        }

        handleRefresh();
    }

    private void applyFilters() {
        if (filteredList == null) return;

        String type   = cbFilterType != null ? cbFilterType.getValue() : "Tous";
        String search = txtSearch    != null ? txtSearch.getText().toLowerCase() : "";

        filteredList.setPredicate(ms -> {
            boolean matchType   = "Tous".equals(type) || type.equals(ms.getType());
            boolean matchSearch = search.isEmpty() ||
                (ms.getMedicamentNom() != null && ms.getMedicamentNom().toLowerCase().contains(search));
            return matchType && matchSearch;
        });

        // Wrapper pour le tri cliquant sur les colonnes
        javafx.collections.transformation.SortedList<MouvementStock> sortedData = new javafx.collections.transformation.SortedList<>(filteredList);
        sortedData.comparatorProperty().bind(mouvementTable.comparatorProperty());
        mouvementTable.setItems(sortedData);
    }

    @FXML
    public void handleShowAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("➕  Nouveau Mouvement de Stock");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:#f8fafc;-fx-font-size:13px;");
        dialog.getDialogPane().setPrefWidth(440);

        List<Medicament> meds = medicamentDAO.findAll();
        ComboBox<Medicament> fMedicament = new ComboBox<>(FXCollections.observableArrayList(meds));
        ComboBox<String>     fType       = new ComboBox<>(FXCollections.observableArrayList("ENTREE", "SORTIE"));
        fType.getSelectionModel().selectFirst();
        TextField fQuantite    = new TextField();
        TextField fCommentaire = new TextField();

        String fs = "-fx-background-color:#f1f5f9;-fx-border-color:#cbd5e1;" +
                    "-fx-border-radius:7;-fx-background-radius:7;-fx-padding:6 10;";
        fMedicament.setStyle(fs); fMedicament.setPromptText("Choisir un médicament *"); fMedicament.setPrefWidth(230);
        fType.setStyle(fs);       fType.setPrefWidth(230);
        fQuantite.setStyle(fs);   fQuantite.setPromptText("Entier > 0 (obligatoire)");
        fCommentaire.setStyle(fs); fCommentaire.setPromptText("Motif, fournisseur... (optionnel)");
        fCommentaire.setPrefWidth(230);

        // Affichage dynamique du stock disponible
        Label lblStock = new Label("Stock disponible : —");
        lblStock.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
        fMedicament.setOnAction(e -> {
            Medicament m = fMedicament.getValue();
            if (m != null) lblStock.setText("Stock disponible : " + m.getQuantite() + " unités");
        });

        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#dc3545;-fx-font-weight:bold;-fx-font-size:12px;-fx-wrap-text:true;");
        lblErr.setMaxWidth(400); lblErr.setMinHeight(28);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(styledLabel("Médicament *"), 0, 0); grid.add(fMedicament,  1, 0);
        grid.add(new Label(""),               0, 1); grid.add(lblStock,     1, 1);
        grid.add(styledLabel("Type *"),        0, 2); grid.add(fType,        1, 2);
        grid.add(styledLabel("Quantité *"),    0, 3); grid.add(fQuantite,    1, 3);
        grid.add(styledLabel("Commentaire"),   0, 4); grid.add(fCommentaire, 1, 4);
        grid.add(lblErr,                       0, 5); GridPane.setColumnSpan(lblErr, 2);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.setStyle("-fx-background-color:#198754;-fx-text-fill:white;" +
                          "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:7 18;");

        // Validation bloquante
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            StringBuilder sb = new StringBuilder();
            Medicament med = fMedicament.getValue();
            String type    = fType.getValue();
            String qStr    = fQuantite.getText().trim();

            if (med == null)  sb.append("• Veuillez sélectionner un médicament.\n");
            if (type == null) sb.append("• Veuillez choisir un type (ENTREE/SORTIE).\n");

            if (qStr.isEmpty()) {
                sb.append("• La quantité est obligatoire.\n");
            } else {
                try {
                    int q = Integer.parseInt(qStr);
                    if (q <= 0)
                        sb.append("• La quantité doit être un entier strictement positif (> 0).\n");
                    else if ("SORTIE".equals(type) && med != null && med.getQuantite() < q)
                        sb.append("• Stock insuffisant ! Stock disponible : "
                            + med.getQuantite() + " unité(s).\n");
                } catch (NumberFormatException ex) {
                    sb.append("• La quantité doit être un nombre entier.\n");
                }
            }

            if (!sb.isEmpty()) {
                lblErr.setText(sb.toString().trim());
                if (fMedicament.getValue() == null)
                    fMedicament.setStyle(fs + "-fx-border-color:#dc3545;-fx-border-width:2;");
                if (qStr.isEmpty())
                    fQuantite.setStyle("-fx-border-color:#dc3545;-fx-border-width:2;" +
                        "-fx-border-radius:7;-fx-background-radius:7;-fx-background-color:#fff5f5;-fx-padding:6 10;");
                event.consume();
            }
        });

        fMedicament.valueProperty().addListener((o,a,b) -> { lblErr.setText(""); fMedicament.setStyle(fs); });
        fQuantite.textProperty().addListener((o,a,b)    -> { lblErr.setText(""); fQuantite.setStyle(fs); });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            Medicament med = fMedicament.getValue();
            MouvementStock ms = new MouvementStock(
                fType.getValue(),
                Integer.parseInt(fQuantite.getText().trim()),
                fCommentaire.getText().trim(),
                med.getId());
            boolean ok = mouvementDAO.add(ms);
            handleRefresh();
            lblStatus.setText(ok
                ? "✅ Mouvement enregistré : " + fType.getValue() + " de " + fQuantite.getText() + " unités."
                : "❌ Erreur lors de l'enregistrement.");
        }
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#475569;-fx-font-weight:bold;");
        return l;
    }

    @FXML
    public void handleRefresh() {
        mouvementList.setAll(mouvementDAO.findAll());
        applyFilters();

        long nbEntrees = mouvementList.stream().filter(m -> "ENTREE".equals(m.getType())).count();
        long nbSorties = mouvementList.stream().filter(m -> "SORTIE".equals(m.getType())).count();

        if (lblStatTotal   != null) lblStatTotal.setText(String.valueOf(mouvementList.size()));
        if (lblStatEntrees != null) lblStatEntrees.setText(String.valueOf(nbEntrees));
        if (lblStatSorties != null) lblStatSorties.setText(String.valueOf(nbSorties));
        if (lblStatus      != null) lblStatus.setText("✅ " + mouvementList.size() + " mouvement(s) chargé(s).");
    }

    @FXML private void handleAdd()    { handleShowAddDialog(); }
    @FXML private void handleDelete() { }
}
