package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Disponibilite;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.DisponibiliteService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AddDispoModalController {

    @FXML
    private DatePicker dpDate;
    @FXML
    private ComboBox<String> cbHeureDebut, cbMinDebut;
    @FXML
    private ComboBox<String> cbHeureFin, cbMinFin;
    @FXML
    private Button btnToday, btnTomorrow, btnThisWeek;

    private DisponibiliteService service = new DisponibiliteService();
    private boolean saved = false;
    private Disponibilite currentDispo; // L'objet en cours d'édition (si null => nouvel ajout)

    @FXML
    public void initialize() {
        // Initialize hours (00 to 23)
        cbHeureDebut.setItems(FXCollections.observableArrayList(
                java.util.stream.IntStream.rangeClosed(0, 23)
                        .mapToObj(i -> String.format("%02d", i))
                        .toList()));
        cbHeureFin.setItems(cbHeureDebut.getItems());

        // Initialize minutes
        cbMinDebut.setItems(FXCollections.observableArrayList("00", "15", "30", "45"));
        cbMinFin.setItems(cbMinDebut.getItems());

        cbHeureDebut.setValue("08");
        cbMinDebut.setValue("00");
        cbHeureFin.setValue("09");
        cbMinFin.setValue("00");
        dpDate.setValue(LocalDate.now());
        updateSidebarStyles(btnToday);
    }

    @FXML
    private void selectToday() {
        dpDate.setValue(LocalDate.now());
        updateSidebarStyles(btnToday);
    }

    @FXML
    private void selectTomorrow() {
        dpDate.setValue(LocalDate.now().plusDays(1));
        updateSidebarStyles(btnTomorrow);
    }

    @FXML
    private void selectNextMonday() {
        LocalDate now = LocalDate.now();
        dpDate.setValue(now.plusDays(7 - now.getDayOfWeek().getValue() + 1));
        updateSidebarStyles(btnThisWeek);
    }

    private void updateSidebarStyles(Button activeBtn) {
        btnToday.getStyleClass().remove("sidebar-item-active");
        btnTomorrow.getStyleClass().remove("sidebar-item-active");
        btnThisWeek.getStyleClass().remove("sidebar-item-active");
        if (activeBtn != null)
            activeBtn.getStyleClass().add("sidebar-item-active");
    }

    /**
     * Pré-remplit le formulaire pour l'édition
     */
    public void setData(Disponibilite d) {
        this.currentDispo = d;
        dpDate.setValue(d.getDateDebut().toLocalDate());

        cbHeureDebut.setValue(String.format("%02d", d.getDateDebut().getHour()));
        cbMinDebut.setValue(String.format("%02d", d.getDateDebut().getMinute()));

        cbHeureFin.setValue(String.format("%02d", d.getDateFin().getHour()));
        cbMinFin.setValue(String.format("%02d", d.getDateFin().getMinute()));

        updateSidebarStyles(null); // Déselectionne les boutons rapides
    }

    @FXML
    private void handleSave() {
        if (dpDate.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner une date.");
            return;
        }

        try {
            LocalDate date = dpDate.getValue();
            LocalTime start = LocalTime.of(Integer.parseInt(cbHeureDebut.getValue()),
                    Integer.parseInt(cbMinDebut.getValue()));
            LocalTime end = LocalTime.of(Integer.parseInt(cbHeureFin.getValue()),
                    Integer.parseInt(cbMinFin.getValue()));

            if (start.isAfter(end) || start.equals(end)) {
                showAlert("Erreur", "L'heure de fin doit être après l'heure de début.");
                return;
            }

            LocalDateTime startDateTime = LocalDateTime.of(date, start);
            LocalDateTime endDateTime = LocalDateTime.of(date, end);

            // Validation : Pas de date passée
            if (startDateTime.isBefore(LocalDateTime.now())) {
                showAlertWarning("Attention", "Vous ne pouvez pas fixer une disponibilité dans le passé.");
                return;
            }

            // Validation : Vérification des chevauchements
            int medecinId = UserSession.getUser().getId();
            Integer excludeId = (currentDispo != null) ? currentDispo.getId() : null;
            if (service.checkOverlap(medecinId, startDateTime, endDateTime, excludeId)) {
                showAlertWarning("Chevauchement", "Créneau déjà publié ou chevauchement détecté.");
                return;
            }

            // Si on édite un objet existant, on met à jour ses champs
            Disponibilite d = (currentDispo != null) ? currentDispo : new Disponibilite();
            d.setDateDebut(startDateTime);
            d.setDateFin(endDateTime);

            if (currentDispo == null) {
                d.setEstReserve(false);
                // Utilisation du médecin connecté via la session
                User medecin = UserSession.getUser();
                d.setMedecin(medecin);
                service.insertOne(d);
            } else {
                service.updateOne(d);
            }

            saved = true;

            closeModal();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format d'heure invalide.");
        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible d'ajouter le créneau : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) dpDate.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlertWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Action non autorisée");
        alert.setContentText(content);
        alert.showAndWait();
    }
}
