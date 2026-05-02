package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.services.PdfService;
import com.hospismart.hospismartdesktop.services.EmailService;
import com.hospismart.hospismartdesktop.utils.NotificationUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.text.Text;

import java.io.IOException;

public class ResumeOrdonnaceController {

    @FXML
    private Text txtConfirmationMessage;
    @FXML
    private Label lblEmailInfo;

    private Consultation currentConsultation;
    private DashboardController dashboardController;

    private final PdfService pdfService = new PdfService();
    private final EmailService emailService = new EmailService();

    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    public void setConsultationData(Consultation consultation) {
        this.currentConsultation = consultation;
        txtConfirmationMessage.setText("Dossier médical mis à jour avec succès.");
        lblEmailInfo.setText("Prêt à imprimer ou envoyer.");
    }

    @FXML
    private void handlePrintPDF() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ordonnacePDF.fxml"));
            javafx.scene.Parent root = loader.load();

            OrdonnancePreviewController controller = loader.getController();
            controller.setConsultation(currentConsultation);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Aperçu de l'Ordonnance");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible d'ouvrir l'aperçu : " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleSendEmail() {
        try {
            // 1. Récupération des données du patient
            com.hospismart.hospismartdesktop.models.User patient = new com.hospismart.hospismartdesktop.services.UserService()
                    .findById(currentConsultation.getPatientId());

            if (patient == null || patient.getEmail() == null || patient.getEmail().isEmpty()) {
                showModernModal("Email Manquant", "Le patient n'a pas d'adresse email.", true);
                return;
            }

            // 2. Génération du PDF
            String pdfPath = pdfService.generateOrdonnancePdf(currentConsultation);

            // 3. ENVOI RÉEL DE L'EMAIL (Cette ligne manquait !)
            emailService.sendEmailWithAttachment(
                    patient.getEmail(),
                    "Votre Ordonnance HospiSmart - Réf #" + currentConsultation.getId(),
                    "Bonjour " + patient.getPrenom() + ",\n\nVeuillez trouver ci-joint votre ordonnance.\n\nCordialement,\nL'équipe HospiSmart",
                    pdfPath);

            // 4. Affichage de la modal MODERNE uniquement
            showModernModal("Email Envoyé",
                    "L'ordonnance a été envoyée avec succès à : " + patient.getEmail(),
                    false);

            // NOTE : Ne plus appeler NotificationUtils ici pour éviter l'ancien design

        } catch (Exception e) {
            showModernModal("Erreur Email", "Échec de l'envoi. Vérifiez votre connexion SMTP.", true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
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
    private void showModernModal(String title, String message, boolean isError) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ModernModal.fxml"));
            javafx.scene.Parent root = loader.load();

            ModernModalController controller = loader.getController();
            controller.setData(title, message, isError, null);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            // Fenêtre parente
            javafx.stage.Window owner = txtConfirmationMessage.getScene().getWindow();
            stage.initOwner(owner);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);

            // CONFIGURATION POUR COUVRIR TOUTE L'APPLICATION
            stage.setX(owner.getX());
            stage.setY(owner.getY());
            stage.setWidth(owner.getWidth());
            stage.setHeight(owner.getHeight());

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}