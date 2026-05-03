package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class UserProfileController {

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telephoneField;

    @FXML
    private PasswordField passwordField;

    private UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            prenomField.setText(currentUser.getPrenom());
            emailField.setText(currentUser.getEmail());
            telephoneField.setText(currentUser.getTelephone());
        }
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nom et l'email sont obligatoires !");
            return;
        }

        currentUser.setNom(nomField.getText());
        currentUser.setPrenom(prenomField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setTelephone(telephoneField.getText());

        if (!passwordField.getText().isEmpty()) {
            currentUser.setPassword(passwordField.getText());
        }

        userService.modifier(currentUser);
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès !");
    }



    @FXML
    void handleFacialRecognition(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/FaceRegister.fxml"));
            javafx.scene.Parent root = loader.load();
            FaceRegisterController controller = loader.getController();
            controller.setUserToRegister(currentUser);
            ((Node) event.getSource()).getScene().setRoot(root);
        } catch (IOException e) {
            String details = e.getCause() != null ? e.getCause().toString() : e.getMessage();
            showAlert(Alert.AlertType.ERROR, "Erreur FXML", "Impossible de charger la page de reconnaissance faciale.\nDétails: " + details);
            e.printStackTrace();
        }
    }

    @FXML
    void handleTwoFactorAuth(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/Setup2FA.fxml"));
            javafx.scene.Parent root = loader.load();
            SetupTwoFactorController controller = loader.getController();
            controller.setNewUserRegistration(currentUser);
            ((Node) event.getSource()).getScene().setRoot(root);
        } catch (IOException e) {
            String details = e.getCause() != null ? e.getCause().toString() : e.getMessage();
            showAlert(Alert.AlertType.ERROR, "Erreur FXML", "Impossible de charger la page de configuration 2FA.\nDétails: " + details);
            e.printStackTrace();
        }
    }

    @FXML
    void handleVoiceHealth(ActionEvent event) {
        com.hospismart.hospismartdesktop.main.JavaFxMain.setRoot("/com/hospismart/hospismartdesktop/VoiceHealth.fxml", null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
