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
        String nom = nomField.getText();
        String prenom = prenomField.getText();
        String email = emailField.getText();
        String telephone = telephoneField.getText();
        String password = passwordField.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de saisie", "Veuillez remplir tous les champs obligatoires.");
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

        try {
            boolean success = userService.ajouter(newUser);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Compte créé avec succès !");
                navigate(event, "/com/hospismart/hospismartdesktop/Login.fxml");
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
