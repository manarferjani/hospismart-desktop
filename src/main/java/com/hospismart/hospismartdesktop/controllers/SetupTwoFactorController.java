package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.TwoFactorAuthService;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.Session;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Contrôleur pour la configuration initiale de la 2FA
 */
public class SetupTwoFactorController {
    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private Label secretKeyLabel;

    @FXML
    private TextField verificationCodeField;

    @FXML
    private Button enableButton;

    @FXML
    private Button skipButton;

    private User currentUser;
    private UserService userService;
    private TwoFactorAuthService twoFactorService;
    private String generatedSecret;
    private boolean isNewUserRegistration = false;
    private boolean initialized = false;  // Flag pour éviter initialize deux fois

    /**
     * Défini si c'est une nouvelle inscription et passe l'utilisateur
     */
    public void setNewUserRegistration(User user) {
        this.isNewUserRegistration = true;
        this.currentUser = user;
        System.out.println("[2FA Setup] Nouvelle inscription pour: " + user.getEmail());
        
        // Si initialize a déjà été appelée, générer le secret maintenant
        if (initialized) {
            generateNewSecret();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("[2FA Setup] initialize() appelée");
        
        userService = new UserService();
        twoFactorService = new TwoFactorAuthService();
        
        // TOUJOURS initialiser les contrôles FXML
        verificationCodeField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,6}")) {
                return change;
            }
            return null;
        }));
        enableButton.setDisable(true);
        
        // Si pas défini par setNewUserRegistration, utiliser la session
        if (currentUser == null) {
            currentUser = Session.getInstance().getCurrentUser();
        }

        if (currentUser == null) {
            System.out.println("[2FA Setup] ⚠️ Utilisateur non défini dans initialize(). Attente de setNewUserRegistration()...");
            initialized = true;
            return;
        }

        // Utilisateur disponible, générer la clé secrète
        System.out.println("[2FA Setup] Utilisateur trouvé: " + currentUser.getEmail());
        generateNewSecret();
        initialized = true;
    }

    /**
     * Génère une nouvelle clé secrète et affiche le QR code
     */
    private void generateNewSecret() {
        try {
            System.out.println("[2FA Setup] Génération d'une nouvelle clé secrète");
            GoogleAuthenticatorKey key = twoFactorService.generateSecret();
            generatedSecret = key.getKey();
            System.out.println("[2FA Setup] Clé secrète générée: " + generatedSecret);

            // Afficher la clé secrète
            secretKeyLabel.setText(generatedSecret);

            // Générer et afficher le QR code en Base64
            System.out.println("[2FA Setup] Appel generateQRCodeBase64 avec email=" + currentUser.getEmail() + ", userId=" + currentUser.getId());
            String qrCodeBase64 = twoFactorService.generateQRCodeBase64(
                currentUser.getEmail(),
                generatedSecret,
                currentUser.getId()
            );
            
            System.out.println("[2FA Setup] QR code Base64 généré, longueur: " + qrCodeBase64.length());
            System.out.println("[2FA Setup] Début du QR code: " + qrCodeBase64.substring(0, Math.min(50, qrCodeBase64.length())));

            // Charger l'image depuis la data URI
            String dataUrl = qrCodeBase64;
            String[] parts = dataUrl.split(",");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Format de data URI invalide: " + dataUrl.substring(0, Math.min(100, dataUrl.length())));
            }
            
            String base64String = parts[1];
            System.out.println("[2FA Setup] Base64 extrait, longueur: " + base64String.length());
            
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64String);
            System.out.println("[2FA Setup] Bytes décodés, longueur: " + decodedBytes.length);
            
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decodedBytes);
            BufferedImage bufferedImage = ImageIO.read(bais);
            System.out.println("[2FA Setup] BufferedImage créée: " + (bufferedImage != null ? "OK (" + bufferedImage.getWidth() + "x" + bufferedImage.getHeight() + ")" : "NULL"));
            
            if (bufferedImage == null) {
                throw new IllegalStateException("BufferedImage est null après ImageIO.read()");
            }
            
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            System.out.println("[2FA Setup] Image FX créée: " + (image != null ? "OK" : "NULL"));
            
            if (image == null) {
                throw new IllegalStateException("Image FX est null après SwingFXUtils.toFXImage()");
            }
            
            qrCodeImageView.setImage(image);
            System.out.println("[2FA Setup] ✅ QR code affiché avec succès!");
        } catch (Exception e) {
            System.err.println("[2FA Setup] ❌ Erreur génération: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la génération du QR code: " + e.getMessage());
        }
    }

    /**
     * Copier la clé secrète dans le presse-papiers
     */
    @FXML
    void handleCopySecret(ActionEvent event) {
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(generatedSecret), null);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Clé copiée dans le presse-papiers");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la copie");
        }
    }

    /**
     * Vérifier le code avant d'activer la 2FA
     */
    @FXML
    void handleVerifyCode(ActionEvent event) {
        String codeText = verificationCodeField.getText().trim();

        if (codeText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer le code 6 chiffres");
            return;
        }

        if (!codeText.matches("\\d{6}")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le code doit contenir 6 chiffres");
            return;
        }

        try {
            int code = Integer.parseInt(codeText);
            if (twoFactorService.verifyCode(generatedSecret, code)) {
                System.out.println("[2FA Setup] ✅ Code vérifié avec succès");
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Code vérifié ! Vous pouvez maintenant activer la 2FA.");
                enableButton.setDisable(false);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Code incorrect. Veuillez réessayer.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le code doit être numérique");
        }
    }

    /**
     * Activer la 2FA pour l'utilisateur
     */
    @FXML
    void handleEnable(ActionEvent event) {
        try {
            System.out.println("[2FA Setup] Activation de la 2FA pour l'utilisateur: " + currentUser.getId());
            
            // Sauvegarder le secret dans la base de données
            if (userService.saveTwoFactorSecret(currentUser.getId(), generatedSecret)) {
                System.out.println("[2FA Setup] ✅ 2FA activée avec succès");
                
                currentUser.setTwoFactorEnabled(true);
                currentUser.setTwoFactorSecret(generatedSecret);
                Session.getInstance().setCurrentUser(currentUser);
                
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "✅ L'authentification à deux facteurs a été activée !\n\n" +
                    "À partir de maintenant, vous devrez entrer un code 2FA à chaque connexion.");
                
                // Si c'est une nouvelle inscription, aller à Login
                // Sinon, retourner au profil utilisateur
                if (isNewUserRegistration) {
                    System.out.println("[2FA Setup] Nouvelle inscription complétée, redirection vers Login");
                    navigateToLogin(event);
                } else {
                    navigateToUserProfile(event);
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'activation de la 2FA");
            }
        } catch (Exception e) {
            System.err.println("[2FA Setup] Erreur: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'activation: " + e.getMessage());
        }
    }

    /**
     * Annuler et revenir au profil (ou login si nouvelle inscription)
     */
    @FXML
    void handleCancel(ActionEvent event) {
        if (isNewUserRegistration) {
            navigateToLogin(event);
        } else {
            navigateToUserProfile(event);
        }
    }

    /**
     * Passer la configuration 2FA et aller au login
     */
    @FXML
    void handleSkip(ActionEvent event) {
        System.out.println("[2FA Setup] Configuration 2FA ignorée par l'utilisateur");
        showAlert(Alert.AlertType.INFORMATION, "Info",
            "Vous pouvez configurer la 2FA ultérieurement depuis votre profil.");
        
        if (isNewUserRegistration) {
            navigateToLogin(event);
        } else {
            navigateToUserProfile(event);
        }
    }

    /**
     * Naviguer vers le profil utilisateur
     */
    private void navigateToUserProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/UserProfile.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[2FA Setup] Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Naviguer vers l'écran de connexion (après nouvelle inscription)
     */
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[2FA Setup] Erreur navigation vers login: " + e.getMessage());
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
