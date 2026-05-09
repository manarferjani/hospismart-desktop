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
import javafx.scene.control.TextInputDialog;
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

            // Vérifier si 2FA est activée
            if (userService.isTwoFactorEnabled(user.getId())) {
                System.out.println("[2FA] Redirection vers vérification 2FA pour: " + user.getEmail());
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/TwoFactorVerification.fxml"));
                    javafx.scene.Parent root = loader.load();
                    TwoFactorAuthController controller = loader.getController();
                    controller.setUserAwaitingVerification(user);
                    Scene currentScene = ((Node) event.getSource()).getScene();
                    currentScene.setRoot(root);
                } catch (IOException e) {
                    System.err.println("[2FA] Erreur lors du chargement de la vérification 2FA: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la vérification 2FA");
                }
            } else {
                // 2FA non activée, continuer normalement
                Session.getInstance().setCurrentUser(user);

                // Utilisation de la navigation centralisée selon le rôle
                com.hospismart.hospismartdesktop.main.JavaFxMain.showDashboard(user);
            }
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

    @FXML
    void handleFaceLogin(ActionEvent event) {
        navigate(event, "/com/hospismart/hospismartdesktop/FaceLogin.fxml");
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Réinitialisation de votre mot de passe");
        dialog.setContentText("Veuillez saisir votre adresse e-mail :");
        dialog.setResizable(true);
        
        // Appliquer le style unifié
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hospismart/hospismartdesktop/style-admin.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("card-pane");
            dialog.getDialogPane().setStyle("-fx-background-color: #f4f6f9; -fx-font-family: 'Segoe UI';");
            dialog.getDialogPane().lookup(".header-panel").setStyle("-fx-background-color: #1565C0;");
            dialog.getDialogPane().lookup(".header-panel .label").setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        } catch (Exception e) {
            System.err.println("Style dialog error: " + e.getMessage());
        }

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String email = result.get().trim();
            
            if (email.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "L'adresse e-mail ne peut pas être vide.");
                return;
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer une adresse e-mail valide.");
                return;
            }

            // Chercher l'utilisateur
            User user = userService.findByEmail(email);
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Aucun compte associé à cette adresse e-mail.\n\nVérifiez que vous avez entré la bonne adresse.");
                return;
            }

            // Générer un nouveau mot de passe
            String newPassword = generateRandomPassword();
            
            // Mettre à jour dans la base de données
            boolean updated = userService.updatePassword(user.getId(), newPassword);
            
            if (!updated) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Impossible de réinitialiser votre mot de passe.\n\nVeuillez contacter le support ou réessayer plus tard.");
                return;
            }

            // Envoyer l'email
            boolean emailSent = com.hospismart.hospismartdesktop.services.EmailService.sendResetPasswordEmail(email, newPassword);
            
            if (emailSent) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                    "Un nouveau mot de passe a été envoyé à votre adresse e-mail.\n\n" +
                    "Veuillez vérifier votre dossier spam si vous ne le recevez pas dans quelques minutes.\n\n" +
                    "Nous vous recommandons de changer ce mot de passe dès votre connexion.");
            } else {
                // Le mot de passe est mis à jour mais l'email n'a pas pu être envoyé
                showAlert(Alert.AlertType.WARNING, "Attention", 
                    "Votre mot de passe a été réinitialisé, mais l'envoi de l'e-mail a échoué.\n\n" +
                    "Causes possibles:\n" +
                    "• Les identifiants de messagerie ne sont pas configurés\n" +
                    "• Une connexion internet est requise\n" +
                    "• Votre compte Gmail nécessite une autorisation spécifique\n\n" +
                    "Contactez le support pour recevoir votre nouveau mot de passe par autre moyen.");
            }
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
            com.hospismart.hospismartdesktop.main.JavaFxMain.setRoot(path, null);
        } catch (Throwable e) {
            System.err.println("Erreur lors de la navigation vers " + path + " : " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur système", "Impossible d'accéder à cette page.\nDétail : " + e.getMessage());
        }
    }
}
