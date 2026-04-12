package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.services.EvenementService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {

    @FXML private Label      formTitle;
    @FXML private Label      errorLabel;

    // ── Form fields ───────────────────────────────────
    @FXML private TextField        titreField;
    @FXML private TextArea         descriptionArea;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private DatePicker       dateDebutPicker;
    @FXML private TextField        heureDebutField;
    @FXML private DatePicker       dateFinPicker;
    @FXML private TextField        heureFinField;
    @FXML private TextField        lieuField;
    @FXML private TextField        budgetField;

    private final EvenementService service = new EvenementService();
    private Evenement evenementToEdit = null;
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.getItems().addAll("réunion", "formation", "visite", "maintenance", "autre");
        statutCombo.getItems().addAll("planifié", "en_cours", "terminé", "annulé");
        typeCombo.setValue("réunion");
        statutCombo.setValue("planifié");
        dateDebutPicker.setValue(LocalDate.now());
        dateFinPicker.setValue(LocalDate.now());
        heureDebutField.setText("09:00");
        heureFinField.setText("11:00");
        hideError();
    }

    /** Called by EvenementBackController before showing the stage. */
    public void setEvenement(Evenement evenement) {
        this.evenementToEdit = evenement;
        if (evenement == null) {
            formTitle.setText("➕  Ajouter un Événement");
            return;
        }
        formTitle.setText("✏  Modifier l'Événement");
        titreField.setText(nvl(evenement.getTitre()));
        descriptionArea.setText(nvl(evenement.getDescription()));
        lieuField.setText(nvl(evenement.getLieu()));
        budgetField.setText(evenement.getBudgetAlloue() > 0
                ? String.format("%.2f", evenement.getBudgetAlloue())
                : "");
        if (evenement.getTypeEvenement() != null) typeCombo.setValue(evenement.getTypeEvenement());
        if (evenement.getStatut()        != null) statutCombo.setValue(evenement.getStatut());
        if (evenement.getDateDebut() != null) {
            dateDebutPicker.setValue(evenement.getDateDebut().toLocalDate());
            heureDebutField.setText(evenement.getDateDebut().format(TF));
        }
        if (evenement.getDateFin() != null) {
            dateFinPicker.setValue(evenement.getDateFin().toLocalDate());
            heureFinField.setText(evenement.getDateFin().format(TF));
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML private void onSave() {
        hideError();

        // ── Validation ────────────────────────────────
        String titre = trimOf(titreField.getText());
        if (titre.isEmpty())      { showError("Le titre est obligatoire."); return; }
        if (titre.length() < 3)   { showError("Le titre doit contenir au moins 3 caractères."); return; }

        String lieu = trimOf(lieuField.getText());
        if (lieu.isEmpty())       { showError("Le lieu est obligatoire."); return; }

        if (typeCombo.getValue()   == null) { showError("Sélectionnez un type d'événement."); return; }
        if (statutCombo.getValue() == null) { showError("Sélectionnez un statut."); return; }
        if (dateDebutPicker.getValue() == null) { showError("La date de début est obligatoire."); return; }
        if (dateFinPicker.getValue()   == null) { showError("La date de fin est obligatoire."); return; }

        // ── Parse times ───────────────────────────────
        LocalDateTime dateDebut, dateFin;
        try {
            dateDebut = LocalDateTime.of(dateDebutPicker.getValue(),
                    LocalTime.parse(trimOf(heureDebutField.getText()), TF));
        } catch (DateTimeParseException ex) {
            showError("Heure de début invalide — utilisez HH:mm (ex: 09:00).");
            return;
        }
        try {
            dateFin = LocalDateTime.of(dateFinPicker.getValue(),
                    LocalTime.parse(trimOf(heureFinField.getText()), TF));
        } catch (DateTimeParseException ex) {
            showError("Heure de fin invalide — utilisez HH:mm (ex: 11:00).");
            return;
        }
        if (!dateFin.isAfter(dateDebut)) {
            showError("La date de fin doit être postérieure à la date de début.");
            return;
        }

        // ── Parse budget ──────────────────────────────
        double budget = 0;
        String budgetText = trimOf(budgetField.getText()).replace(",", ".");
        if (!budgetText.isEmpty()) {
            try {
                budget = Double.parseDouble(budgetText);
                if (budget < 0) { showError("Le budget ne peut pas être négatif."); return; }
            } catch (NumberFormatException ex) {
                showError("Le budget doit être un nombre valide (ex: 500.00).");
                return;
            }
        }

        // ── Build & persist ───────────────────────────
        Evenement e = (evenementToEdit != null) ? evenementToEdit : new Evenement();
        e.setTitre(titre);
        e.setDescription(descriptionArea.getText());
        e.setTypeEvenement(typeCombo.getValue());
        e.setStatut(statutCombo.getValue());
        e.setLieu(lieu);
        e.setDateDebut(dateDebut);
        e.setDateFin(dateFin);
        e.setBudgetAlloue(budget);

        boolean ok = (evenementToEdit != null) ? service.update(e) : service.create(e);
        if (ok) {
            getStage().close();
        } else {
            showError("Erreur lors de l'enregistrement. Vérifiez la connexion à la base de données.");
        }
    }

    @FXML private void onCancel() {
        getStage().close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private Stage getStage() {
        return (Stage) titreField.getScene().getWindow();
    }

    private String trimOf(String s) { return s != null ? s.trim() : ""; }
    private String nvl(String s)    { return s != null ? s : ""; }
}
