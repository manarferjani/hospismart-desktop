package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.services.ConsultationService;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NewConsultationController {

    @FXML
    private Label lblPatientName;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblMotif;

    @FXML
    private TextArea txtExamenClinique;
    @FXML
    private TextArea txtDiagnostic;
    @FXML
    private TextArea txtTraitement;
    @FXML
    private TextArea txtExamensComplementaires;
    @FXML
    private TextArea txtRecommandations;
    @FXML
    private TextArea txtObservations;

    private RendezVous rendezVous;
    private DashboardController dashboardController;
    private final ConsultationService consultationService = new ConsultationService();
    private final RendezVousService rdvService = new RendezVousService();
    private final com.hospismart.hospismartdesktop.services.AIService aiService;

    public NewConsultationController() {
        this.aiService = new com.hospismart.hospismartdesktop.services.AIService(com.hospismart.hospismartdesktop.utils.ApiConfig.GEMINI_API_KEY);
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void setRendezVous(RendezVous rdv) {
        this.rendezVous = rdv;
        lblPatientName.setText(rdv.getPatientName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        lblDate.setText(LocalDateTime.now().format(formatter));

        lblMotif.setText(rdv.getMotif());
    }

    @FXML
    public void handleSave(ActionEvent event) {
        if (rendezVous == null)
            return;

        Consultation consultation = new Consultation();
        consultation.setPatientId(rendezVous.getPatientId());
        consultation.setMedecinId(UserSession.getUser().getId());
        consultation.setRendezVousId(rendezVous.getId());
        consultation.setMotif(rendezVous.getMotif());
        consultation.setExamenClinique(txtExamenClinique.getText());
        consultation.setDiagnostic(txtDiagnostic.getText());
        consultation.setTraitement(txtTraitement.getText());
        consultation.setExamensComplementaires(txtExamensComplementaires.getText());
        consultation.setRecommandations(txtRecommandations.getText());
        consultation.setObservations(txtObservations.getText());
        consultation.setStatut("TERMINEE");
        consultation.setDateHeure(LocalDateTime.now());
        consultation.setPriorite(3); // Default medium

        saveConsultation(consultation);
    }


    @FXML
    public void handleAICheck(ActionEvent event) {
        String diag = txtDiagnostic.getText().trim();
        String trait = txtTraitement.getText().trim();

        if (diag.isEmpty() || trait.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Veuillez remplir le diagnostic et le traitement avant l'analyse IA.");
            return;
        }

        String jsonResult = aiService.analyserPrescription(diag, trait);

        // Extraction des champs
        boolean estCorrect = jsonResult.contains("\"estCorrect\": true")
                || jsonResult.contains("\"estCorrect\":true");
        boolean coherent = jsonResult.contains("\"coherent\": true")
                || jsonResult.contains("\"coherent\":true");
        String suggestion = trait;
        String analyse = "Analyse terminée.";

        java.util.regex.Pattern pSugg = java.util.regex.Pattern.compile(
                "\"suggestion\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        java.util.regex.Matcher mSugg = pSugg.matcher(jsonResult);
        if (mSugg.find())
            suggestion = mSugg.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'");

        java.util.regex.Pattern pAnalyse = java.util.regex.Pattern.compile(
                "\"analyse\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        java.util.regex.Matcher mAnalyse = pAnalyse.matcher(jsonResult);
        if (mAnalyse.find())
            analyse = mAnalyse.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'");

        System.out.println("[AI-Check] estCorrect=" + estCorrect + " | coherent=" + coherent);
        System.out.println("[AI-Check] suggestion=" + suggestion);
        System.out.println("[AI-Check] analyse=" + analyse);

        com.hospismart.hospismartdesktop.utils.AICheckDialog dialog =
                new com.hospismart.hospismartdesktop.utils.AICheckDialog();

        // ── Cas 1 : erreur API — TOUJOURS vérifié en premier ─────────────────
        if (analyse.startsWith("Erreur API") || analyse.startsWith("Erreur technique")
                || analyse.startsWith("Mode offline")
                || jsonResult.contains("\"analyse\": \"Erreur API")
                || jsonResult.contains("\"analyse\":\"Erreur API")) {

            com.hospismart.hospismartdesktop.utils.AICheckDialog.Result r = dialog.show(
                    com.hospismart.hospismartdesktop.utils.AICheckDialog.DialogType.ERROR,
                    "Service IA indisponible",
                    "L'analyse par l'IA n'a pas pu aboutir.",
                    analyse, null,
                    "Continuer sans IA", "Annuler",
                    null, null
            );
            if (r == com.hospismart.hospismartdesktop.utils.AICheckDialog.Result.PRIMARY)
                handleSave(event);

            // ── Cas 2 : incohérence médicale ──────────────────────────────────────

        } else if (!coherent) {

            // Construire le texte de la carte diagnostic/traitement
            String detailCard = "Diagnostic  :  " + diag + "\nTraitement  :  " + trait;

            com.hospismart.hospismartdesktop.utils.AICheckDialog.Result r = dialog.show(
                    com.hospismart.hospismartdesktop.utils.AICheckDialog.DialogType.INCOHERENCE,
                    "Incohérence médicale détectée",
                    "Le traitement prescrit ne semble pas adapté au diagnostic.",
                    detailCard,  // ← carte avec diag + traitement
                    analyse,     // ← explication IA en italique en dessous
                    "Enregistrer quand même", "Corriger le traitement",
                    null, null
            );

            if (r == com.hospismart.hospismartdesktop.utils.AICheckDialog.Result.PRIMARY) {
                handleSave(event);
            } else {
                javafx.application.Platform.runLater(() -> {
                    txtTraitement.requestFocus();
                    txtTraitement.selectAll();
                    txtTraitement.setStyle("""
                        -fx-border-color: #E24B4A;
                        -fx-border-width: 2;
                        -fx-border-radius: 6;
                        -fx-focus-color: #E24B4A;
                        -fx-faint-focus-color: rgba(226,75,74,0.15);
                        """);
                    javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                            "Veuillez corriger le traitement prescrit");
                    tooltip.setStyle("""
                        -fx-background-color: #E24B4A;
                        -fx-text-fill: white;
                        -fx-font-size: 12;
                        -fx-background-radius: 6;
                        -fx-padding: 6 10 6 10;
                        """);
                    tooltip.setAutoHide(true);
                    javafx.scene.control.Tooltip.install(txtTraitement, tooltip);
                    javafx.geometry.Bounds bounds = txtTraitement
                            .localToScreen(txtTraitement.getBoundsInLocal());
                    if (bounds != null) {
                        tooltip.show(txtTraitement,
                                bounds.getMinX(),
                                bounds.getMinY() - 36);
                    }
                    new Thread(() -> {
                        try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(() -> {
                            txtTraitement.setStyle("");
                            javafx.scene.control.Tooltip.uninstall(txtTraitement, tooltip);
                            tooltip.hide();
                        });
                    }).start();
                });
            }

            // ── Cas 3 : faute d'orthographe ───────────────────────────────────────
        } else if (!estCorrect
                && !suggestion.equalsIgnoreCase(trait.trim())
                && !suggestion.isEmpty()
                && !suggestion.contains("Veuillez fournir")) {

            final String suggestionFinal = suggestion;
            com.hospismart.hospismartdesktop.utils.AICheckDialog.Result r = dialog.show(
                    com.hospismart.hospismartdesktop.utils.AICheckDialog.DialogType.CORRECTION,
                    "Correction orthographique",
                    "L'IA a détecté une faute d'orthographe dans le traitement.",
                    null, analyse,
                    "Appliquer la correction", "Ignorer et continuer",
                    trait, suggestionFinal
            );
            if (r == com.hospismart.hospismartdesktop.utils.AICheckDialog.Result.PRIMARY)
                txtTraitement.setText(suggestionFinal);
            handleSave(event);

            // ── Cas 4 : tout est correct ──────────────────────────────────────────
        } else {

            dialog.show(
                    com.hospismart.hospismartdesktop.utils.AICheckDialog.DialogType.SUCCESS,
                    "Traitement validé par l'IA",
                    "Aucune erreur d'orthographe ni incohérence détectée.",
                    null, analyse,
                    "Continuer", null,
                    null, null
            );
            handleSave(event);
        }
    }

    private void saveConsultation(Consultation consultation) {
        try {
            // Force deletion of any existing consultation for this RDV to solve Duplicate
            // Entry issues
            consultationService.deleteByRendezVousId(rendezVous.getId());

            // Now insert (insertOne handles the ON DUPLICATE KEY just in case)
            consultationService.insertOne(consultation);
            rdvService.updateStatut(rendezVous.getId(), "Terminé");

            // Navigate to ResumeOrdonnace
            String viewPath = "/com/hospismart/hospismartdesktop/views/ResumeOrdonnace.fxml";
            URL fxmlLocation = getClass().getResource(viewPath);
            if (fxmlLocation == null) {
                viewPath = "/ResumeOrdonnace.fxml";
                fxmlLocation = getClass().getResource(viewPath);
            }

            if (fxmlLocation == null) {
                throw new IOException("Fichier FXML introuvable : ResumeOrdonnace.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            javafx.scene.Parent root = loader.load();

            ResumeOrdonnaceController controller = loader.getController();
            if (controller != null) {
                controller.setConsultationData(consultation);
                controller.setDashboardController(dashboardController);
            }

            if (dashboardController != null && dashboardController.getContentArea() != null) {
                dashboardController.getContentArea().getChildren().setAll(root);
            }

            if (dashboardController != null && dashboardController.getContentArea() != null) {
                dashboardController.getContentArea().getChildren().setAll(root);
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.showDashboard(event);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
