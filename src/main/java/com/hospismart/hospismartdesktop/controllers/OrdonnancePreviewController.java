package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.services.PdfService;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

public class OrdonnancePreviewController {

    @FXML
    private WebView webView;

    private Consultation currentConsultation;
    private final PdfService pdfService = new PdfService();

    public void setConsultation(Consultation consultation) {
        this.currentConsultation = consultation;
        loadPreview();
    }

    private void loadPreview() {
        try {
            WebEngine engine = webView.getEngine();
            String html = pdfService.getCompiledHtml(currentConsultation);
            engine.loadContent(html);
        } catch (Exception e) {
            showAlert("Erreur d'affichage", "Impossible de générer l'aperçu : " + e.getMessage());
        }
    }

    @FXML
    private void handlePrint() {
        try {
            String path = pdfService.generateOrdonnancePdf(currentConsultation);
            showAlert("Impression réussie", "L'ordonnance a été enregistrée dans vos téléchargements :\n" + path);
            handleClose();
        } catch (Exception e) {
            showAlert("Erreur PDF", "Impossible de générer le fichier PDF : " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}