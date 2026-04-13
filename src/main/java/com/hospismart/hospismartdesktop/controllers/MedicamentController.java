package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.Categorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MedicamentController {

    @FXML private TableView<Medicament> medicamentTable;
    @FXML private TableColumn<Medicament, String>   colNom;
    @FXML private TableColumn<Medicament, Integer>  colQuantite;
    @FXML private TableColumn<Medicament, Integer>  colSeuil;
    @FXML private TableColumn<Medicament, Double>   colPrix;
    @FXML private TableColumn<Medicament, LocalDate> colDate;
    @FXML private TableColumn<Medicament, String>   colCategorie;

    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;
    @FXML private Label lblAlerte;
    @FXML private Label lblStatValeur;
    @FXML private Label lblStatTotal;
    @FXML private ComboBox<Categorie> cbFilterCategorie;
    @FXML private ComboBox<String> cbSortMode;

    private MedicamentDAO medicamentDAO   = new MedicamentDAO();
    private CategorieDAO  categorieDAO   = new CategorieDAO();
    private ObservableList<Medicament> medicamentList = FXCollections.observableArrayList();
    private FilteredList<Medicament>   filteredList;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePeremption"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorieNom"));

        // Couleur rouge si stock en alerte
        colQuantite.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item.toString());
                    Medicament m = getTableView().getItems().get(getIndex());
                    setStyle(item <= m.getSeuilAlerte()
                        ? "-fx-text-fill: #dc3545; -fx-font-weight: bold;"
                        : "-fx-text-fill: #198754;");
                }
            }
        });

        // Colonne Actions (✎ Modifier  ✕ Supprimer)
        TableColumn<Medicament, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(175);
        colActions.setResizable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✎  Modifier");
            private final Button btnDelete = new Button("✕  Supprimer");
            private final javafx.scene.layout.HBox box =
                new javafx.scene.layout.HBox(6, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;");
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle("-fx-background-color:#0d6efd;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));
                btnEdit.setOnMouseExited (e -> btnEdit.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));

                btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;");
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));
                btnDelete.setOnMouseExited (e -> btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));

                box.setAlignment(javafx.geometry.Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    Medicament m = getTableRow().getItem();
                    if (m == null) return;
                    showDialog(m);
                });
                btnDelete.setOnAction(e -> {
                    Medicament m = getTableRow().getItem();
                    if (m == null) return;
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer « " + m.getNom() + " » ?", ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            medicamentDAO.delete(m.getId());
                            handleRefresh();
                            lblStatus.setText("✅ « " + m.getNom() + " » supprimé.");
                        }
                    });
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        medicamentTable.getColumns().add(colActions);

        // Recherche en temps réel  + filtre catégorie
        filteredList = new FilteredList<>(medicamentList, p -> true);
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, newVal) -> applyFilters());
        }
        // Charger la liste des catégories dans le filtre
        if (cbFilterCategorie != null) {
            Categorie all = new Categorie(); all.setId(0); all.setNom("Toutes les catégories");
            ObservableList<Categorie> cats = FXCollections.observableArrayList();
            cats.add(all);
            cats.addAll(categorieDAO.findAll());
            cbFilterCategorie.setItems(cats);
            cbFilterCategorie.getSelectionModel().selectFirst();
            cbFilterCategorie.setOnAction(e -> applyFilters());
        }

        // Configuration du ComboBox de tri explicite
        if (cbSortMode != null) {
            cbSortMode.setItems(FXCollections.observableArrayList(
                "Tri libre", "Prix : Croissant", "Prix : Décroissant",
                "Nom : A-Z", "Quantité : Croissante", "Quantité : Décroissante"
            ));
            cbSortMode.getSelectionModel().selectFirst();
            cbSortMode.setOnAction(e -> applyFilters());
        }

        handleRefresh();
    }

    /** Applique à la fois le filtre texte ET le filtre catégorie */
    private void applyFilters() {
        if (filteredList == null) return;
        
        String search  = txtSearch  != null ? txtSearch.getText().toLowerCase()  : "";
        Categorie cat  = cbFilterCategorie != null ? cbFilterCategorie.getValue() : null;
        int catId      = (cat != null && cat.getId() != 0) ? cat.getId() : 0;

        filteredList.setPredicate(m -> {
            boolean matchSearch = search.isEmpty() ||
                m.getNom().toLowerCase().contains(search) ||
                (m.getCategorieNom() != null && m.getCategorieNom().toLowerCase().contains(search));
            boolean matchCat = (catId == 0) || m.getCategorieId() == catId;
            return matchSearch && matchCat;
        });

        // Wrapper pour activer le tri en cliquant sur les en-têtes de colonnes OU via ComboBox
        javafx.collections.transformation.SortedList<Medicament> sortedData = new javafx.collections.transformation.SortedList<>(filteredList);
        
        String sortMode = cbSortMode != null ? cbSortMode.getValue() : "Tri libre";
        if (!"Tri libre".equals(sortMode)) {
            sortedData.setComparator((m1, m2) -> {
                if ("Prix : Croissant".equals(sortMode)) return Double.compare(m1.getPrixUnitaire(), m2.getPrixUnitaire());
                if ("Prix : Décroissant".equals(sortMode)) return Double.compare(m2.getPrixUnitaire(), m1.getPrixUnitaire());
                if ("Nom : A-Z".equals(sortMode)) return m1.getNom().compareToIgnoreCase(m2.getNom());
                if ("Quantité : Croissante".equals(sortMode)) return Integer.compare(m1.getQuantite(), m2.getQuantite());
                if ("Quantité : Décroissante".equals(sortMode)) return Integer.compare(m2.getQuantite(), m1.getQuantite());
                return 0;
            });
        } else {
            // Mode normal : l'utilisateur clique sur les en-têtes des colonnes
            sortedData.comparatorProperty().bind(medicamentTable.comparatorProperty());
        }
        
        medicamentTable.setItems(sortedData);
    }

    // ── POPUP D'AJOUT / MODIFICATION ─────────────────────────────────────
    @FXML
    public void handleShowAddDialog() {
        showDialog(null); // null = mode AJOUT
    }

    private void showDialog(Medicament existing) {
        boolean isEdit = (existing != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "✎  Modifier le médicament" : "➕  Nouveau médicament");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType(isEdit ? "Enregistrer" : "Ajouter",
                                            ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:#f8fafc;-fx-font-size:13px;");
        dialog.getDialogPane().setPrefWidth(460);

        // ── Champs ──────────────────────────────────────────────────────
        TextField   fNom      = new TextField(isEdit ? existing.getNom() : "");
        TextField   fQuantite = new TextField(isEdit ? String.valueOf(existing.getQuantite()) : "");
        TextField   fSeuil    = new TextField(isEdit ? String.valueOf(existing.getSeuilAlerte()) : "");
        TextField   fPrix     = new TextField(isEdit ? String.valueOf(existing.getPrixUnitaire()) : "");
        DatePicker  fDate     = new DatePicker(isEdit ? existing.getDatePeremption() : null);
        ComboBox<Categorie> fCategorie = new ComboBox<>(
            FXCollections.observableArrayList(categorieDAO.findAll()));
        if (isEdit) fCategorie.getItems().stream()
            .filter(c -> c.getId() == existing.getCategorieId())
            .findFirst().ifPresent(fCategorie::setValue);

        String fs = "-fx-background-color:#f1f5f9;-fx-border-color:#cbd5e1;" +
                    "-fx-border-radius:7;-fx-background-radius:7;-fx-padding:6 10;";
        fNom.setStyle(fs);      fNom.setPromptText("Nom du médicament (obligatoire)");
        fQuantite.setStyle(fs); fQuantite.setPromptText("Entier ≥ 0");
        fSeuil.setStyle(fs);    fSeuil.setPromptText("Entier ≥ 0");
        fPrix.setStyle(fs);     fPrix.setPromptText("Décimal ≥ 0 (ex: 12.50)");
        fDate.setStyle(fs);     fDate.setPromptText("JJ/MM/AAAA");
        fCategorie.setStyle(fs); fCategorie.setPromptText("Catégorie"); fCategorie.setPrefWidth(200);

        // ── Label d'erreur inline (rouge) ────────────────────────────────
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#dc3545;-fx-font-weight:bold;-fx-font-size:12px;-fx-wrap-text:true;");
        lblErr.setMaxWidth(400);
        lblErr.setMinHeight(30);

        // ── Grid ─────────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(styledLabel("Nom *"),           0, 0); grid.add(fNom,       1, 0);
        grid.add(styledLabel("Quantité *"),       0, 1); grid.add(fQuantite,  1, 1);
        grid.add(styledLabel("Seuil Alerte *"),   0, 2); grid.add(fSeuil,     1, 2);
        grid.add(styledLabel("Prix Unitaire *"),  0, 3); grid.add(fPrix,      1, 3);
        grid.add(styledLabel("Date Péremption"),  0, 4); grid.add(fDate,      1, 4);
        grid.add(styledLabel("Catégorie"),        0, 5); grid.add(fCategorie, 1, 5);
        grid.add(lblErr,                          0, 6); GridPane.setColumnSpan(lblErr, 2);
        dialog.getDialogPane().setContent(grid);

        // ── Style bouton OK ──────────────────────────────────────────────
        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.setStyle("-fx-background-color:#198754;-fx-text-fill:white;" +
                          "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:7 18;");

        // ── Bloquer la fermeture si invalide ─────────────────────────────
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String errs = validateMedicament(fNom, fQuantite, fSeuil, fPrix, fDate,
                                             isEdit ? existing.getId() : 0);
            if (!errs.isEmpty()) {
                lblErr.setText(errs);
                // Marquer les champs en erreur en rouge
                highlightIfEmpty(fNom);
                highlightIfNotInt(fQuantite); highlightIfNotInt(fSeuil);
                highlightIfNotDouble(fPrix);
                event.consume(); // Empêche la fermeture du dialog
            }
        });

        // ── Réinitialiser erreur au moindre changement ───────────────────
        fNom.textProperty().addListener((o,a,b)      -> lblErr.setText(""));
        fQuantite.textProperty().addListener((o,a,b) -> lblErr.setText(""));
        fSeuil.textProperty().addListener((o,a,b)    -> lblErr.setText(""));
        fPrix.textProperty().addListener((o,a,b)     -> lblErr.setText(""));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            Medicament m = isEdit ? existing : new Medicament();
            m.setNom(fNom.getText().trim());
            m.setQuantite(Integer.parseInt(fQuantite.getText().trim()));
            m.setSeuilAlerte(Integer.parseInt(fSeuil.getText().trim()));
            m.setPrixUnitaire(Double.parseDouble(fPrix.getText().trim().replace(",", ".")));
            m.setDatePeremption(fDate.getValue());
            if (fCategorie.getValue() != null) m.setCategorieId(fCategorie.getValue().getId());

            boolean ok = isEdit ? medicamentDAO.update(m) : medicamentDAO.add(m);
            handleRefresh();
            lblStatus.setText(ok
                ? (isEdit ? "✅ Médicament modifié !" : "✅ Médicament ajouté !")
                : "❌ Erreur lors de l'opération.");
        }
    }

    /** Retourne une chaîne d'erreurs (vide si tout est valide) */
    private String validateMedicament(TextField fNom, TextField fQte, TextField fSeuil,
                                       TextField fPrix, DatePicker fDate, int excludeId) {
        StringBuilder sb = new StringBuilder();
        String nom = fNom.getText().trim();

        if (nom.isEmpty())
            sb.append("• Le nom est obligatoire.\n");
        else if (nom.length() < 2)
            sb.append("• Le nom doit contenir au moins 2 caractères.\n");
        else if (medicamentDAO.existsByNom(nom, excludeId))
            sb.append("• Un médicament avec ce nom existe déjà.\n");

        if (fQte.getText().trim().isEmpty()) sb.append("• La quantité est obligatoire.\n");
        else { try { int v = Integer.parseInt(fQte.getText().trim());
            if (v < 0) sb.append("• La quantité doit être ≥ 0.\n");
        } catch (NumberFormatException e) { sb.append("• La quantité doit être un nombre entier.\n"); } }

        if (fSeuil.getText().trim().isEmpty()) sb.append("• Le seuil d'alerte est obligatoire.\n");
        else { try { int v = Integer.parseInt(fSeuil.getText().trim());
            if (v < 0) sb.append("• Le seuil doit être ≥ 0.\n");
        } catch (NumberFormatException e) { sb.append("• Le seuil doit être un nombre entier.\n"); } }

        if (fPrix.getText().trim().isEmpty()) sb.append("• Le prix unitaire est obligatoire.\n");
        else { try { double v = Double.parseDouble(fPrix.getText().trim().replace(",","."));
            if (v < 0) sb.append("• Le prix doit être ≥ 0.\n");
        } catch (NumberFormatException e) { sb.append("• Le prix doit être un nombre décimal (ex: 12.50).\n"); } }

        if (fDate.getValue() != null && fDate.getValue().isBefore(java.time.LocalDate.now()))
            sb.append("• La date de péremption doit être dans le futur.\n");

        return sb.toString().trim();
    }

    private void highlightIfEmpty(TextField f) {
        if (f.getText().trim().isEmpty())
            f.setStyle("-fx-border-color:#dc3545;-fx-border-width:2;-fx-border-radius:7;-fx-background-radius:7;-fx-background-color:#fff5f5;-fx-padding:6 10;");
    }
    private void highlightIfNotInt(TextField f) {
        try { Integer.parseInt(f.getText().trim()); }
        catch (Exception e) { f.setStyle("-fx-border-color:#dc3545;-fx-border-width:2;-fx-border-radius:7;-fx-background-radius:7;-fx-background-color:#fff5f5;-fx-padding:6 10;"); }
    }
    private void highlightIfNotDouble(TextField f) {
        try { Double.parseDouble(f.getText().trim().replace(",",".")); }
        catch (Exception e) { f.setStyle("-fx-border-color:#dc3545;-fx-border-width:2;-fx-border-radius:7;-fx-background-radius:7;-fx-background-color:#fff5f5;-fx-padding:6 10;"); }
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#475569;-fx-font-weight:bold;");
        return l;
    }

    // ── EXPORT CSV ────────────────────────────────────────────────────────
    @FXML
    public void handleExportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les médicaments en CSV");
        chooser.setInitialFileName("medicaments_export.csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        java.io.File file = chooser.showSaveDialog(medicamentTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // En-tête
            pw.println("Nom,Quantite,Seuil_Alerte,Prix_Unitaire,Date_Peremption,Categorie,Statut");
            // Données (liste filtrée visible)
            ObservableList<Medicament> visible =
                filteredList != null ? filteredList : medicamentList;
            for (Medicament m : visible) {
                String statut = m.getQuantite() <= m.getSeuilAlerte() ? "ALERTE" : "OK";
                pw.printf("\"%s\",%d,%d,%.2f,\"%s\",\"%s\",%s%n",
                    m.getNom(),
                    m.getQuantite(),
                    m.getSeuilAlerte(),
                    m.getPrixUnitaire(),
                    m.getDatePeremption() != null ? m.getDatePeremption().toString() : "",
                    m.getCategorieNom() != null ? m.getCategorieNom() : "",
                    statut);
            }
            lblStatus.setText("✅ Export CSV réussi : " + visible.size() + " médicaments → " + file.getName());
        } catch (IOException e) {
            lblStatus.setText("❌ Erreur export : " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        medicamentList.setAll(medicamentDAO.findAll());
        if (filteredList != null) applyFilters();
        else medicamentTable.setItems(medicamentList);

        long nbAlerte = medicamentList.stream()
            .filter(m -> m.getQuantite() <= m.getSeuilAlerte()).count();
        double valeurStock = medicamentList.stream()
            .mapToDouble(m -> m.getPrixUnitaire() * m.getQuantite()).sum();

        if (lblAlerte != null)
            lblAlerte.setText(nbAlerte > 0 ? "⚠️ " + nbAlerte + " en alerte" : "✅ Stocks OK");
        if (lblStatValeur != null)
            lblStatValeur.setText(String.format("Valeur totale du stock : %.2f TND", valeurStock));
        if (lblStatTotal != null)
            lblStatTotal.setText(medicamentList.size() + " médicaments");
        if (lblStatus != null)
            lblStatus.setText("✅ " + medicamentList.size() + " médicaments chargés.");
    }

    // Méthodes legacy conservées pour compatibilité
    @FXML private void handleAdd()    { showDialog(null); }
    @FXML private void handleUpdate() { }
    @FXML private void handleDelete() { }
}
