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

public class AddUserBackController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    private UserService userService = new UserService();

    @FXML
    void handleRegister(ActionEvent event) {
        // Reset styles
        nomField.setStyle("");
        prenomField.setStyle("");
        emailField.setStyle("");
        telephoneField.setStyle("");
        passwordField.setStyle("");
        roleComboBox.setStyle("");

        String nom = nomField.getText() != null ? nomField.getText().trim() : "";
        String prenom = prenomField.getText() != null ? prenomField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String telephone = telephoneField.getText() != null ? telephoneField.getText().trim() : "";
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        boolean isValid = true;

        if (nom.isEmpty()) {
            nomField.setStyle("-fx-border-color: red;");
            isValid = false;
        }
        if (prenom.isEmpty()) {
            prenomField.setStyle("-fx-border-color: red;");
            isValid = false;
        }
        if (role == null || role.isEmpty()) {
            roleComboBox.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation email
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation téléphone (exactement 8 chiffres)
        if (telephone.isEmpty() || !telephone.matches("\\d{8}")) {
            telephoneField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation mot de passe (min 6 caractères, 1 chiffre, 1 caractère spécial)
        if (password == null || password.isEmpty() || password.length() < 6 || !password.matches(".*\\d.*") || !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        if (!isValid) {
            showAlert(Alert.AlertType.ERROR, "Erreur de saisie", "Veuillez corriger les champs en rouge.\n- Le téléphone doit contenir exactement 8 chiffres.\n- L'email doit être valide.\n- Le mot de passe doit contenir au moins 6 caractères, un chiffre et un caractère spécial.");
            return;
        }

        User newUser = new User();
        newUser.setNom(nom);
        newUser.setPrenom(prenom);
        newUser.setEmail(email);
        newUser.setTelephone(telephone);
        newUser.setPassword(password);
        newUser.setType(role); // the exact literal string e.g. "ROLE_MEDECIN"

        try {
            boolean success = userService.ajouter(newUser);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "L'utilisateur a été ajouté avec succès au système.");
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "L'ajout a échoué. Vérifiez vos données ou la base de données.");
            }
        } catch (java.sql.SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de créer le compte.\nDétail : " + e.getMessage());
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
