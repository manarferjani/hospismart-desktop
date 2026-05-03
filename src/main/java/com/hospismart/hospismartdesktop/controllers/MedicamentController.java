package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.MouvementStockDAO;
import com.hospismart.hospismartdesktop.services.MailService;
import com.hospismart.hospismartdesktop.services.ImageAIService;
import com.hospismart.hospismartdesktop.services.QRCodeService;
import com.hospismart.hospismartdesktop.services.PredictionService;
import com.hospismart.hospismartdesktop.models.MouvementStock;
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
import java.util.Optional;
import com.hospismart.hospismartdesktop.services.CategorieDAO;

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

        // ===== COLONNE IMAGE =====
        TableColumn<Medicament, String> colImage = new TableColumn<>("Image");
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageFilename"));
        colImage.setPrefWidth(60);
        colImage.setCellFactory(col -> new TableCell<>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();

            {
                imageView.setFitWidth(45);
                imageView.setFitHeight(45);
                imageView.setPreserveRatio(true);
                setStyle("-fx-cursor: hand;");
                // Clic sur la cellule pour agrandir l'image
                setOnMouseClicked(e -> {
                    Medicament m = getTableRow() != null ? getTableRow().getItem() : null;
                    if (m != null && m.getImageFilename() != null && !m.getImageFilename().isEmpty()) {
                        java.io.File imgFile = new java.io.File("generated_images/" + m.getImageFilename());
                        showImagePopup(m.getNom(), imgFile.getAbsolutePath());
                    }
                });
            }

            @Override
            protected void updateItem(String imageFilename, boolean empty) {
                super.updateItem(imageFilename, empty);
                if (empty || imageFilename == null || imageFilename.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        java.io.File imgFile = new java.io.File("generated_images/" + imageFilename);
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                            imgFile.toURI().toString(), 45, 45, true, true);
                        imageView.setImage(img);
                        setGraphic(imageView);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });
        // Ajouter la colonne au début du tableau
        medicamentTable.getColumns().add(0, colImage);

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

        // Colonne Actions (✐ Modifier  ✕ Supprimer  🤖 IA  📱 QR)
        TableColumn<Medicament, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(330);
        colActions.setResizable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✐  Modifier");
            private final Button btnDelete = new Button("✕  Supprimer");
            private final Button btnAI     = new Button("🤖 IA");
            private final Button btnQR     = new Button("📱 QR");
            private final javafx.scene.layout.HBox box =
                new javafx.scene.layout.HBox(5, btnEdit, btnDelete, btnAI, btnQR);

            {
                btnEdit.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;");
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle("-fx-background-color:#0d6efd;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));
                btnEdit.setOnMouseExited (e -> btnEdit.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));

                btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;");
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));
                btnDelete.setOnMouseExited (e -> btnDelete.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc3545;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 10;"));

                btnAI.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;");
                btnAI.setOnMouseEntered(e -> btnAI.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;"));
                btnAI.setOnMouseExited (e -> btnAI.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;"));

                btnQR.setStyle("-fx-background-color:#d1fae5;-fx-text-fill:#065f46;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;");
                btnQR.setOnMouseEntered(e -> btnQR.setStyle("-fx-background-color:#065f46;-fx-text-fill:white;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;"));
                btnQR.setOnMouseExited (e -> btnQR.setStyle("-fx-background-color:#d1fae5;-fx-text-fill:#065f46;-fx-background-radius:6;-fx-border-radius:6;-fx-cursor:hand;-fx-padding:4 8;-fx-font-weight:bold;"));

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

                // ===== BOUTON IA : Générer image avec HuggingFace =====
                btnAI.setOnAction(e -> {
                    Medicament m = getTableRow().getItem();
                    if (m == null) return;
                    btnAI.setText("⏳...");
                    btnAI.setDisable(true);
                    lblStatus.setText("🤖 Génération IA en cours pour " + m.getNom() + "...");

                    final int medId = m.getId();
                    final String medNom = m.getNom();
                    final String medCat = m.getCategorieNom();

                    new Thread(() -> {
                        ImageAIService aiService = new ImageAIService();
                        String imagePath = aiService.genererImage(medNom, medCat, medId);

                        javafx.application.Platform.runLater(() -> {
                            btnAI.setText("🤖 IA");
                            btnAI.setDisable(false);

                            if (imagePath != null) {
                                // Mettre à jour le nom du fichier image en BDD
                                java.io.File imgFile = new java.io.File(imagePath);
                                m.setImageFilename(imgFile.getName());
                                medicamentDAO.update(m);

                                // Rafraîchir le tableau pour afficher l'image dans la colonne
                                handleRefresh();
                                lblStatus.setText("✅ Image IA générée pour " + medNom + " !");
                            } else {
                                lblStatus.setText("❌ Échec de la génération IA pour " + medNom);
                            }
                        });
                    }).start();
                });

                // ===== BOUTON QR : Générer QR Code via API =====
                btnQR.setOnAction(e -> {
                    Medicament m = getTableRow().getItem();
                    if (m == null) return;
                    btnQR.setText("⏳");
                    btnQR.setDisable(true);
                    lblStatus.setText("📱 Génération QR Code pour " + m.getNom() + "...");

                    final Medicament medQR = m;
                    new Thread(() -> {
                        QRCodeService qrService = new QRCodeService();
                        String qrPath = qrService.genererQRCode(
                            medQR.getNom(), medQR.getQuantite(), medQR.getSeuilAlerte(),
                            medQR.getPrixUnitaire(),
                            medQR.getDatePeremption() != null ? medQR.getDatePeremption().toString() : "",
                            medQR.getCategorieNom(), medQR.getId()
                        );

                        javafx.application.Platform.runLater(() -> {
                            btnQR.setText("📱 QR");
                            btnQR.setDisable(false);

                            if (qrPath != null) {
                                lblStatus.setText("✅ QR Code généré pour " + medQR.getNom() + " !");
                                showImagePopup("📱 QR Code : " + medQR.getNom(), qrPath);
                            } else {
                                lblStatus.setText("❌ Échec de la génération QR pour " + medQR.getNom());
                            }
                        });
                    }).start();
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

            if (ok) {
                String statusMsg = isEdit ? "✅ Médicament modifié !" : "✅ Médicament ajouté !";

                // ===== ALERTE EMAIL : vérifier si le stock est sous le seuil =====
                if (m.getQuantite() <= m.getSeuilAlerte()) {
                    final String baseMsg = statusMsg;
                    final Medicament medAlerte = m;
                    new Thread(() -> {
                        MailService mailService = new MailService();
                        boolean emailOk = mailService.envoyerAlerteRuptureStock(medAlerte);
                        javafx.application.Platform.runLater(() -> {
                            if (emailOk) {
                                lblStatus.setText(baseMsg + " | 📧 Alerte email envoyée !");
                            } else {
                                lblStatus.setText(baseMsg + " | ⚠️ Échec envoi email.");
                            }
                        });
                    }).start();
                    statusMsg += " | 📧 Envoi alerte en cours...";
                }

                lblStatus.setText(statusMsg);
            } else {
                lblStatus.setText("❌ Erreur lors de l'opération.");
            }
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

    // ===== PRÉDICTION IA DE RUPTURE DE STOCK =====
    @FXML
    private void handlePrediction() {
        lblStatus.setText("🧠 Analyse prédictive en cours...");

        new Thread(() -> {
            PredictionService predictionService = new PredictionService();
            MouvementStockDAO mouvDAO = new MouvementStockDAO();

            // 1. Calculer les prédictions pour chaque médicament
            java.util.List<PredictionService.PredictionResult> predictions = new java.util.ArrayList<>();
            for (Medicament med : medicamentList) {
                java.util.List<MouvementStock> mouvements = mouvDAO.findByMedicamentId(med.getId());
                PredictionService.PredictionResult pred = predictionService.predire(med, mouvements);
                predictions.add(pred);
            }

            // Trier par risque (les plus critiques en premier)
            predictions.sort((a, b) -> Integer.compare(a.joursAvantRupture, b.joursAvantRupture));

            // 2. Générer le rapport IA
            String rapportIA = predictionService.genererRapportIA(predictions);

            // 3. Afficher les résultats sur le thread JavaFX
            final java.util.List<PredictionService.PredictionResult> finalPreds = predictions;
            final String finalRapport = rapportIA;

            javafx.application.Platform.runLater(() -> {
                lblStatus.setText("✅ Analyse prédictive terminée !");
                showPredictionDialog(finalPreds, finalRapport);
            });
        }).start();
    }

    private void showPredictionDialog(java.util.List<PredictionService.PredictionResult> predictions, String rapportIA) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🧠 Rapport d'Analyse IA");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(850);
        dialog.getDialogPane().setPrefHeight(680);
        dialog.getDialogPane().setStyle("-fx-background-color: #f8fafc; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Bouton de fermeture stylisé
        javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        }

        javafx.scene.layout.VBox mainBox = new javafx.scene.layout.VBox(20);
        mainBox.setPadding(new Insets(25));
        mainBox.setStyle("-fx-background-color: transparent;");

        // --- EN-TÊTE ---
        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox(15);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #5b21b6); -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(124,58,237,0.3), 10, 0, 0, 4);");
        Label titreIcon = new Label("🤖");
        titreIcon.setStyle("-fx-font-size: 36px;");
        javafx.scene.layout.VBox titreTexts = new javafx.scene.layout.VBox(2);
        Label titre = new Label("Intelligence Artificielle");
        titre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sousTitre = new Label("Analyse prédictive experte des risques de rupture de stock");
        sousTitre.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8dbfa;");
        titreTexts.getChildren().addAll(titre, sousTitre);
        headerBox.getChildren().addAll(titreIcon, titreTexts);
        mainBox.getChildren().add(headerBox);

        // --- SECTION RAPPORT IA ---
        javafx.scene.layout.VBox rapportBox = new javafx.scene.layout.VBox(10);
        Label lblRapport = new Label("📝 Décryptage et Recommandations (Généré par IA)");
        lblRapport.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        javafx.scene.control.TextArea txtRapport = new javafx.scene.control.TextArea();
        txtRapport.setWrapText(true);
        txtRapport.setEditable(false);
        txtRapport.setPrefHeight(180);
        txtRapport.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-background-color: transparent; -fx-control-inner-background: white; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-background-radius: 12;");
        txtRapport.setText(rapportIA != null && !rapportIA.isEmpty() ? rapportIA.trim() : "⚠️ Impossible de générer le rapport textuel intelligent. Veuillez vérifier votre connexion.");

        rapportBox.getChildren().addAll(lblRapport, txtRapport);
        mainBox.getChildren().add(rapportBox);

        // --- SECTION TABLEAU ---
        javafx.scene.layout.VBox tableBox = new javafx.scene.layout.VBox(10);
        Label lblTableau = new Label("📊 Détails Mathématiques des Prédictions");
        lblTableau.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        javafx.scene.control.TableView<PredictionService.PredictionResult> table = new javafx.scene.control.TableView<>();
        table.setPrefHeight(230);
        table.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, String> colMed = new javafx.scene.control.TableColumn<>("Médicament");
        colMed.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().nomMedicament));
        colMed.setPrefWidth(200);
        colMed.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-alignment: center-left; -fx-padding: 0 0 0 10;");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, Number> colStock = new javafx.scene.control.TableColumn<>("Stock");
        colStock.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().stockActuel));
        colStock.setPrefWidth(80);
        colStock.setStyle("-fx-alignment: center;");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, String> colConso = new javafx.scene.control.TableColumn<>("Conso/Jour");
        colConso.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%.1f", data.getValue().consommationJour)));
        colConso.setPrefWidth(100);
        colConso.setStyle("-fx-alignment: center;");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, String> colJours = new javafx.scene.control.TableColumn<>("Autonomie");
        colJours.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().joursAvantRupture >= 0 ? data.getValue().joursAvantRupture + " j" : "N/A"));
        colJours.setPrefWidth(100);
        colJours.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, String> colDate = new javafx.scene.control.TableColumn<>("Date Rupture");
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().dateRuptureEstimee));
        colDate.setPrefWidth(120);
        colDate.setStyle("-fx-alignment: center;");

        javafx.scene.control.TableColumn<PredictionService.PredictionResult, String> colRisque = new javafx.scene.control.TableColumn<>("Criticité");
        colRisque.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().niveauRisque));
        colRisque.setPrefWidth(160);
        colRisque.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    PredictionService.PredictionResult p = getTableRow().getItem();
                    if (p != null) {
                        Label badge = new Label(item.toUpperCase());
                        String color = p.couleurRisque.equals("red") ? "#dc3545" : (p.couleurRisque.equals("orange") ? "#d97706" : "#059669");
                        String bg = p.couleurRisque.equals("red") ? "#fee2e2" : (p.couleurRisque.equals("orange") ? "#fef3c7" : "#d1fae5");
                        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-size: 11px;");

                        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(badge);
                        box.setAlignment(javafx.geometry.Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    }
                }
            }
        });

        table.getColumns().addAll(colMed, colStock, colConso, colJours, colDate, colRisque);
        table.getItems().addAll(predictions);

        tableBox.getChildren().addAll(lblTableau, table);
        mainBox.getChildren().add(tableBox);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(mainBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");
        dialog.getDialogPane().setContent(scroll);

        dialog.showAndWait();
    }

    private Label styledLabel2(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    private void showImagePopup(String medNom, String imagePath) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🤖 Image IA : " + medNom);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        try {
            java.io.File file = new java.io.File(imagePath);
            javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString());
            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
            imgView.setFitWidth(500);
            imgView.setPreserveRatio(true);

            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, imgView);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            box.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(box);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("❌ Erreur affichage image : " + e.getMessage());
        }
    }
}
