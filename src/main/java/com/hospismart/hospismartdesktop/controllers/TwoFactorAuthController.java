package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.TwoFactorAuthService;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour la vérification du code 2FA (TOTP) à la connexion
 */
public class TwoFactorAuthController {
    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    private User userAwaitingVerification;
    private UserService userService;
    private TwoFactorAuthService twoFactorService;
    private int attemptCount = 0;
    private static final int MAX_ATTEMPTS = 4;
    private Stage currentStage = null;

    @FXML
    public void initialize() {
        userService = new UserService();
        twoFactorService = new TwoFactorAuthService();
        errorLabel.setText("");

        // Limiter le champ à 6 caractères numériques
        codeField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,6}")) {
                return change;
            }
            return null;
        }));
    }

    /**
     * Définit l'utilisateur en attente de vérification 2FA
     */
    public void setUserAwaitingVerification(User user) {
        this.userAwaitingVerification = user;
        System.out.println("[2FA] Vérification 2FA pour l'utilisateur: " + user.getEmail());
    }

    /**
     * Vérifie le code 2FA saisi par l'utilisateur
     */
    @FXML
    void handleVerify(ActionEvent event) {
        // Store the stage for later use
        if (currentStage == null && event != null && event.getSource() instanceof Node) {
            currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        }

        String codeText = codeField.getText().trim();

        // Validation
        if (codeText.isEmpty()) {
            showError("❌ Veuillez entrer le code 2FA");
            return;
        }

        if (!codeText.matches("\\d{6}")) {
            showError("❌ Le code doit contenir 6 chiffres");
            return;
        }

        if (userAwaitingVerification == null) {
            showError("❌ Erreur: Utilisateur non défini");
            return;
        }

        try {
            int code = Integer.parseInt(codeText);
            String secret = userService.getTwoFactorSecret(userAwaitingVerification.getId());

            if (secret == null || secret.isEmpty()) {
                showError("❌ 2FA non configurée pour cet utilisateur");
                return;
            }

            // Vérifier le code TOTP
            if (twoFactorService.verifyCode(secret, code)) {
                System.out.println("[2FA] ✅ Code 2FA vérifié avec succès");
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Authentification 2FA réussie !");

                // Sauvegarder la session et naviguer vers le profil
                Session.getInstance().setCurrentUser(userAwaitingVerification);
                navigateToUserProfile(event);
            } else {
                attemptCount++;
                System.out.println("[2FA] ❌ Tentative échouée " + attemptCount + " / " + MAX_ATTEMPTS);

                if (attemptCount >= MAX_ATTEMPTS) {
                    // Désactiver le compte après trop de tentatives
                    System.out.println("[2FA] 🔒 Compte désactivé après " + MAX_ATTEMPTS + " tentatives pour: " + userAwaitingVerification.getEmail());

                    boolean deactivated = userService.setActive(userAwaitingVerification.getId(), false);

                    if (deactivated) {
                        showAlert(Alert.AlertType.ERROR, "Compte Désactivé",
                            "Votre compte a été désactivé suite à trop de tentatives de vérification 2FA échouées.\n\n" +
                                "Veuillez contacter l'administrateur pour réactiver votre compte.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Une erreur est survenue lors de la désactivation du compte.\n\nVeuillez contacter l'administrateur.");
                    }

                    // Rediriger vers la page de login
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> navigateToScene("/com/hospismart/hospismartdesktop/Login.fxml"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    int remaining = MAX_ATTEMPTS - attemptCount;
                    showError("❌ Code incorrect. (" + remaining + " tentatives restantes)");
                }
            }
        } catch (NumberFormatException e) {
            showError("❌ Le code doit être numérique");
        }
    }

    /**
     * Utiliser un code de secours
     */
    @FXML
    void handleUseBackupCode(ActionEvent event) {
        // TODO: Implémenter la vérification des codes de secours
        showAlert(Alert.AlertType.INFORMATION, "Code de secours",
            "Fonctionnalité à venir.\n\nContactez l'administrateur pour obtenir un nouveau code.");
    }

    /**
     * Annuler et revenir à la connexion
     */
    @FXML
    void handleCancel(ActionEvent event) {
        navigate(event, "/com/hospismart/hospismartdesktop/Login.fxml");
    }

    /**
     * Afficher un message d'erreur
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f;");
    }

    /**
     * Naviguer vers le profil utilisateur
     */
    private void navigateToUserProfile(ActionEvent event) {
        try {
            String userRole = userAwaitingVerification.getType();
            String viewPath = "/com/hospismart/hospismartdesktop/UserProfile.fxml";

            if (userRole != null && (userRole.contains("ROLE_ADMIN") || userRole.contains("ROLE_MEDECIN"))) {
                viewPath = "/com/hospismart/hospismartdesktop/BackOfficeUsers.fxml";
            }

            navigate(event, viewPath);
        } catch (Exception e) {
            System.err.println("[2FA] Erreur navigation: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la navigation: " + e.getMessage());
        }
    }

    /**
     * Naviguer vers une autre page avec ActionEvent
     */
    private void navigate(ActionEvent event, String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[2FA] Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Naviguer vers une autre page sans ActionEvent (utilise la stage stockée)
     */
    private void navigateToScene(String path) {
        try {
            if (currentStage == null) {
                System.err.println("[2FA] Erreur: Stage non disponible");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load());
            currentStage.setScene(scene);
            currentStage.show();
        } catch (IOException e) {
            System.err.println("[2FA] Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
