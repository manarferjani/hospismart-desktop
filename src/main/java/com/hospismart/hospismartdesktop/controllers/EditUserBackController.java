package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditUserBackController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    private UserService userService = new UserService();
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            telephoneField.setText(user.getTelephone());
            
            // Format du rôle propre : ["ROLE_ADMIN"] -> ROLE_ADMIN dans le ComboBox
            String cleanRole = user.getType();
            if (cleanRole != null) {
                if (cleanRole.contains("ROLE_ADMIN")) roleComboBox.setValue("ROLE_ADMIN");
                else if (cleanRole.contains("ROLE_MEDECIN")) roleComboBox.setValue("ROLE_MEDECIN");
                else roleComboBox.setValue("ROLE_PATIENT");
            } else {
                roleComboBox.setValue("ROLE_PATIENT");
            }
        }
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        if (nomField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty() || telephoneField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de saisie", "Le nom, l'email et le téléphone sont obligatoires !");
            return;
        }

        if (!emailField.getText().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Email Invalide", "Veuillez entrer une adresse e-mail valide.");
            return;
        }

        if (!telephoneField.getText().trim().matches("\\d{8,15}")) {
            showAlert(Alert.AlertType.ERROR, "Téléphone Invalide", "Le numéro de téléphone doit contenir entre 8 et 15 chiffres.");
            return;
        }

        if (!passwordField.getText().isEmpty() && passwordField.getText().length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Mot de passe faible", "Le nouveau mot de passe doit comporter au moins 6 caractères.");
            return;
        }

        currentUser.setNom(nomField.getText());
        currentUser.setPrenom(prenomField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setTelephone(telephoneField.getText());
        currentUser.setType(roleComboBox.getValue());

        // Garder l'ancien mdp si aucun nouveau fourni
        if (!passwordField.getText().isEmpty()) {
            currentUser.setPassword(passwordField.getText());
        }

        boolean success = userService.modifier(currentUser);
        if(success) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "L'utilisateur " + currentUser.getNom() + " a été modifié avec succès !");
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "La modification a échoué.");
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
