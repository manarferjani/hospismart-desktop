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

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private UserService userService = new UserService();

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            if (!user.isActive()) {
                showAlert(Alert.AlertType.WARNING, "Compte désactivé", "Votre compte a été désactivé par un administrateur.");
                return;
            }

            Session.getInstance().setCurrentUser(user);

            // Redirection selon le type (les rôles sont au format JSON ex: ["ROLE_ADMIN"])
            String viewPath = "";
            String userRole = user.getType();
            if (userRole != null && (userRole.contains("ROLE_ADMIN") || userRole.contains("ROLE_MEDECIN"))) {
                viewPath = "/com/hospismart/hospismartdesktop/BackOfficeUsers.fxml";
            } else {
                viewPath = "/com/hospismart/hospismartdesktop/UserProfile.fxml"; // ROLE_PATIENT
            }

            navigate(event, viewPath);
        } else {
            String errorMsg = UserService.lastLoginError;
            if (errorMsg == null || errorMsg.isEmpty()) errorMsg = "Mot de passe incorrect ou erreur inconnue.";
            showAlert(Alert.AlertType.ERROR, "Détail de l'échec", errorMsg + "\nAssurez-vous de relancer 'mvn clean javafx:run'.");
        }
    }

    @FXML
    void handleGoRegister(ActionEvent event) {
        navigate(event, "/com/hospismart/hospismartdesktop/Register.fxml");
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
