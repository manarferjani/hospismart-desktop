package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

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

    @FXML
    void handleRegister(ActionEvent event) {
        // Reset styles
        nomField.setStyle("");
        prenomField.setStyle("");
        emailField.setStyle("");
        telephoneField.setStyle("");
        passwordField.setStyle("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telephone = telephoneField.getText().trim();
        String password = passwordField.getText();

        boolean isValid = true;

        if (nom.isEmpty()) {
            nomField.setStyle("-fx-border-color: red;");
            isValid = false;
        }
        if (prenom.isEmpty()) {
            prenomField.setStyle("-fx-border-color: red;");
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
        if (password.isEmpty() || password.length() < 6 || !password.matches(".*\\d.*") || !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
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
        newUser.setType("ROLE_PATIENT");  // Type par défaut pour les nouvelles inscriptions

        try {
            boolean success = userService.ajouter(newUser);
            if (success) {
                User registeredUser = userService.findByEmail(newUser.getEmail());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Compte créé avec succès ! Enregistrez maintenant votre visage.");
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/FaceRegister.fxml"));
                    Scene scene = new Scene(loader.load());
                    FaceRegisterController controller = loader.getController();
                    controller.setUserToRegister(registeredUser);
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    navigate(event, "/com/hospismart/hospismartdesktop/Login.fxml");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "L'ajout a échoué (aucune ligne modifiée).");
            }
        } catch (java.sql.SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de créer le compte.\nDétail de l'erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleGoLogin(ActionEvent event) {
        navigate(event, "/com/hospismart/hospismartdesktop/Login.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void navigate(ActionEvent event, String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
