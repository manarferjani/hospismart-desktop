package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Disponibilite;
import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.AIService;
import com.hospismart.hospismartdesktop.services.DisponibiliteService;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class FinaliserRdvController {

    @FXML private Label lblMedecinValue;
    @FXML private Label lblDateValue;
    @FXML private TextArea txtMotif;

    private Disponibilite selectedDispo;
    private final RendezVousService rdvService = new RendezVousService();
    private final DisponibiliteService dispoService = new DisponibiliteService();
    private final AIService aiService;

    public FinaliserRdvController() {
        this.aiService = new AIService(com.hospismart.hospismartdesktop.utils.ApiConfig.GEMINI_API_KEY);
    }

    @FXML
    public void initialize() {
        if (UserSession.getUser() == null) {
            User userTest = new User();
            userTest.setId(1);
            userTest.setNom("Testeur");
            userTest.setPrenom("JavaFX");
            UserSession.login(userTest);
        }
    }

    public void setDonneesRdv(Disponibilite dispo) {
        this.selectedDispo = dispo;
        if (dispo.getMedecin() != null) {
            String nomComplet = (dispo.getMedecin().getNom() + " " + dispo.getMedecin().getPrenom()).trim();
            lblMedecinValue.setText("Dr. " + nomComplet);
        } else {
            lblMedecinValue.setText("ID: " + dispo.getMedecinId());
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        lblDateValue.setText(dispo.getDateDebut().format(formatter));
    }

    @FXML
    private void handleConfirmer() {
        if (txtMotif.getText().trim().isEmpty()) {
            showModernModal("Champ Requis", "Veuillez préciser le motif de votre rendez-vous.", true);
            return;
        }

        try {
            RendezVous rv = new RendezVous();
            rv.setDatetime(selectedDispo.getDateDebut());
            String motif = txtMotif.getText().trim();
            rv.setMotif(motif);
            rv.setStatut("En attente");
            rv.setPatientId(UserSession.getUser().getId());
            rv.setMedecinId(selectedDispo.getMedecinId());
            rv.setDisponibiliteId(selectedDispo.getId());

            int prioriteIA = aiService.calculerPriorite(motif);
            rv.setPriorite(prioriteIA);

            rdvService.insertOne(rv);
            dispoService.updateStatut(selectedDispo.getId(), true);

            // Succès : On affiche la modal moderne
            // Succès : On affiche la modal moderne
            showModernModal(
                    "Demande Envoyée",
                    "Votre rendez-vous avec " + lblMedecinValue.getText() + " a été enregistré. Veuillez attendre la confirmation de votre médecin.",
                    false
            );
            // On ferme la fenêtre de réservation après le "Got it" de la modal
            txtMotif.getScene().getWindow().hide();

        } catch (SQLException e) {
            showModernModal("Erreur de Base de Données", "Impossible d'enregistrer le RDV : " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        if (txtMotif.getScene() != null) {
            txtMotif.getScene().getWindow().hide();
        }
    }

    /**
     * Méthode générique pour afficher la nouvelle modal moderne avec fond sombre
     */
    private void showModernModal(String title, String message, boolean isError) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModernModal.fxml"));
            Parent root = loader.load();

            ModernModalController controller = loader.getController();
            controller.setData(title, message, isError, null);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            // Récupérer la fenêtre parente pour l'overlay
            Window owner = txtMotif.getScene().getWindow();
            stage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            // Ajustement de la taille pour l'effet Overlay (fond sombre)
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
