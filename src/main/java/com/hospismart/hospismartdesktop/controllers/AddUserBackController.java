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
        String nom = nomField.getText() != null ? nomField.getText().trim() : "";
        String prenom = prenomField.getText() != null ? prenomField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String telephone = telephoneField.getText() != null ? telephoneField.getText().trim() : "";
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de saisie", "Veuillez remplir tous les champs obligatoires, y compris le rôle.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Email Invalide", "Veuillez entrer une adresse e-mail valide.");
            return;
        }

        if (!telephone.matches("\\d{8,15}")) {
            showAlert(Alert.AlertType.ERROR, "Téléphone Invalide", "Le numéro de téléphone doit contenir entre 8 et 15 chiffres.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Mot de passe faible", "Le mot de passe doit comporter au moins 6 caractères.");
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
