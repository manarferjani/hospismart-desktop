package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.models.Inscription;
import com.hospismart.hospismartdesktop.services.EvenementService;
import com.hospismart.hospismartdesktop.services.InscriptionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EvenementFrontController implements Initializable {

    @FXML private FlowPane         cardsContainer;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label            totalLabel;

    private final EvenementService   service     = new EvenementService();
    private final InscriptionService inscService = new InscriptionService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeFilter.getItems().addAll("Tous", "réunion", "formation", "visite", "maintenance", "autre");
        typeFilter.setValue("Tous");
        loadData();
    }

    /** Called by MainController when user switches to this tab. */
    public void loadData() {
        renderCards("", "");
    }

    @FXML private void onSearch() {
        String kw   = searchField.getText();
        String type = "Tous".equals(typeFilter.getValue()) ? "" : typeFilter.getValue();
        renderCards(kw, type);
    }

    @FXML private void onRefresh() {
        searchField.clear();
        typeFilter.setValue("Tous");
        loadData();
    }

    /** Navigate back to the welcome screen. */
    @FXML private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/welcome-view.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setTitle("HospiSmart — Bienvenue");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Card rendering ────────────────────────────────────────────────────────

    private void renderCards(String keyword, String type) {
        List<Evenement> list = service.search(keyword, type, "");
        cardsContainer.getChildren().clear();
        for (Evenement e : list) cardsContainer.getChildren().add(buildCard(e));
        if (totalLabel != null) totalLabel.setText(list.size() + " événement(s)");
    }

    /**
     * Build a card that matches the web Bootstrap card layout with an "S'inscrire" button.
     */
    private VBox buildCard(Evenement e) {
        VBox card = new VBox(0);
        card.getStyleClass().add("event-card");
        card.setMinWidth(290);
        card.setMaxWidth(320);

        VBox body = new VBox(6);
        body.getStyleClass().add("card-body");

        // ── Row 1: Title + Status badge ──────────────────────────────
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);

        Label titre = new Label(nvl(e.getTitre()));
        titre.getStyleClass().add("card-title");
        titre.setWrapText(true);
        titre.setMaxWidth(200);
        HBox.setHgrow(titre, Priority.ALWAYS);

        Label statusBadge = new Label();
        statusBadge.getStyleClass().add("card-status-badge");
        String statusText, statusStyle;
        switch (nvl(e.getStatut())) {
            case "planifié"  -> { statusText = "Planifié";  statusStyle = "-fx-background-color:#fef3c7; -fx-text-fill:#92400e;"; }
            case "en_cours"  -> { statusText = "En cours";  statusStyle = "-fx-background-color:#dbeafe; -fx-text-fill:#1e40af;"; }
            case "terminé"   -> { statusText = "Terminé";   statusStyle = "-fx-background-color:#dcfce7; -fx-text-fill:#166534;"; }
            case "annulé"    -> { statusText = "Annulé";    statusStyle = "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;"; }
            default          -> { statusText = nvl(e.getStatut()); statusStyle = "-fx-background-color:#ecf0f1; -fx-text-fill:#2c3e50;"; }
        }
        statusBadge.setText(statusText);
        statusBadge.setStyle(statusStyle);

        titleRow.getChildren().addAll(titre, statusBadge);
        body.getChildren().add(titleRow);

        // ── Row 2: Type (small muted) ─────────────────────────────────
        if (e.getTypeEvenement() != null) {
            Label type = new Label("🏷  " + e.getTypeEvenement());
            type.getStyleClass().add("card-type-text");
            body.getChildren().add(type);
        }

        // ── Row 3: Description truncated to ~100 chars ────────────────
        String desc = nvl(e.getDescription());
        if (!desc.isBlank()) {
            String truncated = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
            Label descLabel = new Label(truncated);
            descLabel.getStyleClass().add("card-description");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(295);
            body.getChildren().add(descLabel);
        }

        // ── Divider ───────────────────────────────────────────────────
        Region sep = new Region();
        sep.setStyle("-fx-background-color:#e2e8f0; -fx-min-height:1; -fx-pref-height:1; -fx-max-height:1;");
        sep.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(sep);

        // ── Date/Location/Budget rows ─────────────────────────────────
        if (e.getDateDebut() != null)
            body.getChildren().add(infoRow("📅", "Début : " + e.getDateDebut().format(FMT)));
        if (e.getDateFin() != null)
            body.getChildren().add(infoRow("✅", "Fin    : " + e.getDateFin().format(FMT)));
        if (e.getLieu() != null && !e.getLieu().isBlank())
            body.getChildren().add(infoRow("📍", e.getLieu()));
        if (e.getBudgetAlloue() > 0)
            body.getChildren().add(infoRow("💰", String.format("Budget : %.0f TND", e.getBudgetAlloue())));

        // ── Inscription count ─────────────────────────────────────────
        int nbInscrits = inscService.countByEvenement(e.getId());
        body.getChildren().add(infoRow("👥", nbInscrits + " inscrit(s)"));

        // ── Divider before button ─────────────────────────────────────
        Region sep2 = new Region();
        sep2.setStyle("-fx-background-color:#e2e8f0; -fx-min-height:1; -fx-pref-height:1; -fx-max-height:1;");
        sep2.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(sep2);

        // ── S'inscrire button ─────────────────────────────────────────
        Button inscrireBtn = new Button("📝  S'inscrire");
        inscrireBtn.getStyleClass().add("btn-inscrire");
        inscrireBtn.setMaxWidth(Double.MAX_VALUE);
        inscrireBtn.setOnAction(ev -> showInscriptionDialog(e));

        // Disable for cancelled or finished events
        if ("annulé".equals(e.getStatut()) || "terminé".equals(e.getStatut())) {
            inscrireBtn.setDisable(true);
            inscrireBtn.setText("Inscriptions fermées");
        }

        VBox.setMargin(inscrireBtn, new Insets(6, 0, 0, 0));
        body.getChildren().add(inscrireBtn);

        card.getChildren().add(body);
        return card;
    }

    // ── Inscription Dialog ────────────────────────────────────────────────────

    private void showInscriptionDialog(Evenement event) {
        Dialog<Inscription> dialog = new Dialog<>();
        dialog.setTitle("S'inscrire à : " + event.getTitre());
        dialog.setHeaderText("📝  Inscription à l'événement");

        // ── Dialog Buttons ───────────────────────────────
        ButtonType inscrireType = new ButtonType("S'inscrire", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(inscrireType, ButtonType.CANCEL);

        // ── Form layout ──────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));

        TextField nomField = new TextField();
        nomField.setPromptText("Votre nom complet");
        nomField.setPrefWidth(280);

        TextField emailField = new TextField();
        emailField.setPromptText("votre.email@exemple.com");
        emailField.setPrefWidth(280);

        TextField telField = new TextField();
        telField.setPromptText("+216 XX XXX XXX (optionnel)");
        telField.setPrefWidth(280);

        Label eventLabel = new Label(event.getTitre());
        eventLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label(event.getDateDebut() != null ? event.getDateDebut().format(FMT) : "—");
        dateLabel.setStyle("-fx-text-fill: #64748b;");

        Label lieuLabel = new Label(event.getLieu() != null ? event.getLieu() : "—");
        lieuLabel.setStyle("-fx-text-fill: #64748b;");

        grid.add(new Label("Événement :"), 0, 0);
        grid.add(eventLabel, 1, 0);
        grid.add(new Label("Date :"), 0, 1);
        grid.add(dateLabel, 1, 1);
        grid.add(new Label("Lieu :"), 0, 2);
        grid.add(lieuLabel, 1, 2);

        // Separator
        Region dialogSep = new Region();
        dialogSep.setStyle("-fx-background-color:#e2e8f0; -fx-min-height:1; -fx-pref-height:1;");
        GridPane.setColumnSpan(dialogSep, 2);
        grid.add(dialogSep, 0, 3);

        grid.add(new Label("Nom *:"), 0, 4);
        grid.add(nomField, 1, 4);
        grid.add(new Label("Email *:"), 0, 5);
        grid.add(emailField, 1, 5);
        grid.add(new Label("Téléphone :"), 0, 6);
        grid.add(telField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Disable the button until required fields are filled
        Node inscrireButton = dialog.getDialogPane().lookupButton(inscrireType);
        inscrireButton.setDisable(true);

        // Validation: enable button when nom and email are filled
        Runnable validate = () -> {
            boolean valid = !nomField.getText().trim().isEmpty()
                    && !emailField.getText().trim().isEmpty()
                    && emailField.getText().contains("@");
            inscrireButton.setDisable(!valid);
        };
        nomField.textProperty().addListener((obs, o, n) -> validate.run());
        emailField.textProperty().addListener((obs, o, n) -> validate.run());

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == inscrireType) {
                Inscription insc = new Inscription();
                insc.setEvenementId(event.getId());
                insc.setNomParticipant(nomField.getText().trim());
                insc.setEmailParticipant(emailField.getText().trim());
                insc.setTelephoneParticipant(telField.getText().trim().isEmpty() ? null : telField.getText().trim());
                return insc;
            }
            return null;
        });

        Optional<Inscription> result = dialog.showAndWait();
        result.ifPresent(inscription -> {
            // Check if already registered
            if (inscService.isDejaInscrit(event.getId(), inscription.getEmailParticipant())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Déjà inscrit");
                alert.setHeaderText(null);
                alert.setContentText("Vous êtes déjà inscrit(e) à cet événement avec l'email : "
                        + inscription.getEmailParticipant());
                alert.showAndWait();
                return;
            }

            boolean ok = inscService.inscrire(inscription);
            if (ok) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription réussie ✅");
                alert.setHeaderText(null);
                alert.setContentText("Félicitations ! Vous êtes inscrit(e) à l'événement \""
                        + event.getTitre() + "\".\n\nUn total de "
                        + inscService.countByEvenement(event.getId()) + " participant(s) inscrit(s).");
                alert.showAndWait();
                // Refresh cards to update inscription counts
                loadData();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText(null);
                alert.setContentText("Impossible de finaliser l'inscription. Veuillez réessayer.");
                alert.showAndWait();
            }
        });
    }

    private HBox infoRow(String icon, String text) {
        HBox row = new HBox(6);
        row.getStyleClass().add("card-info-row");
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("card-icon");
        iconLabel.setPrefWidth(18);
        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("card-info-text");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(260);
        row.getChildren().addAll(iconLabel, textLabel);
        return row;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
