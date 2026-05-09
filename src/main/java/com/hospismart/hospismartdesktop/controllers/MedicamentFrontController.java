package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Categorie;
import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MedicamentFrontController implements Initializable {

    @FXML
    private FlowPane flowPaneMedicaments;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cmbCategorie;

    @FXML
    private Label lblTotal;

    private MedicamentDAO medicamentDAO = new MedicamentDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private ObservableList<Medicament> medicamentList = FXCollections.observableArrayList();
    private FilteredList<Medicament> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCategories();
        loadMedicaments();

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        if (cmbCategorie != null) {
            cmbCategorie.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
    }

    private void loadCategories() {
        if (cmbCategorie == null) return;
        List<Categorie> cats = categorieDAO.findAll();
        ObservableList<String> catNames = FXCollections.observableArrayList();
        catNames.add("Toutes les catégories");
        for (Categorie c : cats) {
            catNames.add(c.getNom());
        }
        cmbCategorie.setItems(catNames);
        cmbCategorie.setValue("Toutes les catégories");
    }

    private void loadMedicaments() {
        List<Medicament> list = medicamentDAO.findAll();
        medicamentList.setAll(list);
        filteredList = new FilteredList<>(medicamentList, b -> true);
        
        displayCards(filteredList);
    }

    private void applyFilters() {
        String keyword = txtSearch.getText();
        String selectedCategory = cmbCategorie != null ? cmbCategorie.getValue() : "Toutes les catégories";

        filteredList.setPredicate(m -> {
            boolean matchesSearch = true;
            if (keyword != null && !keyword.isEmpty()) {
                String lower = keyword.toLowerCase();
                matchesSearch = (m.getNom() != null && m.getNom().toLowerCase().contains(lower)) ||
                                (m.getCategorieNom() != null && m.getCategorieNom().toLowerCase().contains(lower));
            }

            boolean matchesCategory = true;
            if (selectedCategory != null && !selectedCategory.equals("Toutes les catégories")) {
                matchesCategory = m.getCategorieNom() != null && m.getCategorieNom().equals(selectedCategory);
            }

            return matchesSearch && matchesCategory;
        });

        displayCards(filteredList);
    }

    private void displayCards(List<Medicament> list) {
        if (flowPaneMedicaments == null) return;
        flowPaneMedicaments.getChildren().clear();
        if (lblTotal != null) {
            lblTotal.setText(list.size() + " résultats");
        }

        for (Medicament m : list) {
            VBox card = createCard(m);
            flowPaneMedicaments.getChildren().add(card);
        }
    }

    private VBox createCard(Medicament m) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);");
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        // -- TOP ROW : Icon, Title/Cat, Badge --
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setStyle("-fx-background-color: #e0e7ff; -fx-background-radius: 50; -fx-min-width: 45; -fx-min-height: 45; -fx-max-width: 45; -fx-max-height: 45;");
        Label iconLabel = new Label("💊");
        iconLabel.setStyle("-fx-font-size: 20; -fx-text-fill: #3b82f6;");
        iconPane.getChildren().add(iconLabel);

        VBox titleBox = new VBox(2);
        Label lblNom = new Label(m.getNom());
        lblNom.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        
        Label lblCat = new Label(m.getCategorieNom() != null && !m.getCategorieNom().isEmpty() ? m.getCategorieNom() : "Non classé");
        lblCat.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblNom, lblCat);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label lblBadge = new Label();
        lblBadge.setStyle("-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
        if (m.getQuantite() <= m.getSeuilAlerte()) {
            lblBadge.setText("! Critique");
            lblBadge.setStyle(lblBadge.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
        } else {
            lblBadge.setText("✓ Dispo");
            lblBadge.setStyle(lblBadge.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;");
        }

        topRow.getChildren().addAll(iconPane, titleBox, spacer, lblBadge);

        // -- MIDDLE ROW : QUANTITE / PRIX --
        HBox statsBox = new HBox();
        statsBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 10;");
        statsBox.setAlignment(Pos.CENTER);

        VBox qteBox = new VBox(5);
        qteBox.setAlignment(Pos.CENTER);
        Label lblQteTitle = new Label("QUANTITÉ");
        lblQteTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        Label lblQteValue = new Label(String.valueOf(m.getQuantite()));
        lblQteValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + (m.getQuantite() <= m.getSeuilAlerte() ? "#dc2626" : "#10b981") + ";");
        qteBox.getChildren().addAll(lblQteTitle, lblQteValue);
        HBox.setHgrow(qteBox, Priority.ALWAYS);

        Line divider = new Line(0, 0, 0, 30);
        divider.setStyle("-fx-stroke: #e2e8f0; -fx-stroke-width: 1;");

        VBox prixBox = new VBox(5);
        prixBox.setAlignment(Pos.CENTER);
        Label lblPrixTitle = new Label("PRIX");
        lblPrixTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        Label lblPrixValue = new Label(String.format("%.2f TND", m.getPrixUnitaire()));
        lblPrixValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        prixBox.getChildren().addAll(lblPrixTitle, lblPrixValue);
        HBox.setHgrow(prixBox, Priority.ALWAYS);

        statsBox.getChildren().addAll(qteBox, divider, prixBox);

        // -- BOTTOM ROW : DATE --
        HBox bottomRow = new HBox(5);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label lblCal = new Label("🗓️");
        lblCal.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label lblDate = new Label();
        if (m.getDatePeremption() != null) {
            lblDate.setText("Exp: " + m.getDatePeremption().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            if (m.getDatePeremption().isBefore(java.time.LocalDate.now())) {
                lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                lblDate.setText("Expiré: " + m.getDatePeremption().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        } else {
            lblDate.setText("Pas de date d'expiration");
            lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        }
        bottomRow.getChildren().addAll(lblCal, lblDate);

        card.getChildren().addAll(topRow, statsBox, bottomRow);

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20, 0, 5, 10); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);"));

        return card;
    }

    @FXML
    private void actualiser() {
        loadMedicaments();
        txtSearch.clear();
        if (cmbCategorie != null) cmbCategorie.setValue("Toutes les catégories");
    }
}
