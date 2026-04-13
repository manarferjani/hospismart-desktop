package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.HelloApplication;
import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Contrôleur du Frontoffice public : affichage des médicaments sous forme de cartes
 */
public class FrontofficeController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;
    @FXML private Label lblAlerte;

    private MedicamentDAO medicamentDAO = new MedicamentDAO();
    private ObservableList<Medicament> medicamentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadMedicaments();

        // Recherche en temps réel
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndDisplay(newVal));
        }
    }

    private void loadMedicaments() {
        List<Medicament> meds = medicamentDAO.findAll();
        medicamentList.setAll(meds);
        displayCards(medicamentList);

        long nbAlerte = meds.stream().filter(m -> m.getQuantite() <= m.getSeuilAlerte()).count();
        if (lblTotal != null)
            lblTotal.setText(meds.size() + " médicaments disponibles");
        if (lblAlerte != null && nbAlerte > 0)
            lblAlerte.setText("⚠️ " + nbAlerte + " en rupture de stock");
        else if (lblAlerte != null)
            lblAlerte.setText("");
    }

    private void filterAndDisplay(String query) {
        if (query == null || query.isEmpty()) {
            displayCards(medicamentList);
            return;
        }
        ObservableList<Medicament> filtered = FXCollections.observableArrayList(
            medicamentList.filtered(m -> m.getNom().toLowerCase().contains(query.toLowerCase())
                    || (m.getCategorieNom() != null && m.getCategorieNom().toLowerCase().contains(query.toLowerCase())))
        );
        displayCards(filtered);
        lblTotal.setText(filtered.size() + " résultat(s) pour \"" + query + "\"");
    }

    private void displayCards(ObservableList<Medicament> list) {
        cardsContainer.getChildren().clear();

        for (Medicament m : list) {
            VBox card = buildCard(m);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox buildCard(Medicament m) {
        boolean enAlerte = m.getQuantite() <= m.getSeuilAlerte();

        VBox card = new VBox(6);
        card.setPrefWidth(210);
        card.setPrefHeight(160);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);" +
            "-fx-cursor: hand;"
        );

        // Indicateur couleur haut de carte
        String topColor = enAlerte ? "#dc3545" : "#0d6efd";
        StackPane indicator = new StackPane();
        indicator.setPrefHeight(4);
        indicator.setStyle("-fx-background-color: " + topColor + "; -fx-background-radius: 3;");

        // Nom médicament
        Label nomLabel = new Label(m.getNom());
        nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(180);

        // Catégorie
        String catText = m.getCategorieNom() != null ? m.getCategorieNom() : "Sans catégorie";
        Label catLabel = new Label("🏷  " + catText);
        catLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Stock badge
        String stockStyle = enAlerte
            ? "-fx-background-color: #fee2e2; -fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;"
            : "-fx-background-color: #d1fae5; -fx-text-fill: #198754; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;";
        String stockText = enAlerte
            ? "⚠ Stock: " + m.getQuantite()
            : "✓ Stock: " + m.getQuantite();
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle(stockStyle);

        // Prix
        Label prixLabel = new Label(String.format("%.2f TND", m.getPrixUnitaire()));
        prixLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0d6efd;");

        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bottomRow.getChildren().addAll(stockLabel, spacer, prixLabel);

        card.getChildren().addAll(indicator, nomLabel, catLabel, spacer, bottomRow);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(13,110,253,0.2), 16, 0, 0, 5);" +
            "-fx-cursor: hand;" +
            "-fx-border-color: #0d6efd; -fx-border-width: 1; -fx-border-radius: 14;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);" +
            "-fx-cursor: hand;"
        ));

        return card;
    }

    @FXML
    public void handleRefresh() {
        txtSearch.clear();
        loadMedicaments();
    }

    @FXML
    public void handleGoAdmin() {
        HelloApplication.showAdmin();
    }
}
