package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.FaceRecognitionService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class FaceRegisterController {
    @FXML
    private ImageView cameraView;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button captureButton;
    
    @FXML
    private Button skipButton;

    private FaceRecognitionService faceService;
    private AnimationTimer animationTimer;
    private User userToRegister;
    private boolean faceRegistered = false;

    public void setUserToRegister(User user) {
        this.userToRegister = user;
    }

    @FXML
    public void initialize() {
        try {
            this.faceService = new FaceRecognitionService();
            
            // Vérifier que la caméra est disponible
            if (!FaceRecognitionService.isCameraAvailable()) {
                showAlert(Alert.AlertType.WARNING, "Avertissement", 
                    "Aucune caméra détectée.\n\nVous pourrez enregistrer votre visage plus tard depuis votre profil.");
                captureButton.setDisable(true);
                return;
            }

            // Initialiser la caméra
            if (!faceService.initializeCamera()) {
                showAlert(Alert.AlertType.WARNING, "Avertissement", 
                    "Impossible d'accéder à la caméra.\n\nVous pourrez enregistrer votre visage plus tard.");
                captureButton.setDisable(true);
                return;
            }

            statusLabel.setText("✅ Caméra initialisée - Positionnez votre visage");
            startCameraStream();
        } catch (Exception e) {
            System.err.println("[FaceRegister] Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Démarre le flux vidéo en direct
     */
    private void startCameraStream() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    Mat frame = faceService.captureFrame();
                    if (frame != null && !frame.empty()) {
                        // Détecter les visages
                        MatOfRect faceDetections = faceService.detectFaces(frame);
                        
                        // Dessiner les rectangles
                        faceService.drawFaceDetections(frame, faceDetections);
                        
                        if (faceDetections.toArray().length > 0) {
                            statusLabel.setText("👤 Visage détecté - Cliquez sur 'Enregistrer'");
                            captureButton.setDisable(false);
                        } else {
                            statusLabel.setText("❌ Aucun visage détecté");
                            captureButton.setDisable(true);
                        }

                        Image image = matToImage(frame);
                        if (image != null) {
                            cameraView.setImage(image);
                        }
                        
                        frame.release();
                    }
                } catch (Exception e) {
                    System.err.println("[FaceRegister] Erreur flux vidéo: " + e.getMessage());
                }
            }
        };
        animationTimer.start();
    }

    /**
     * Enregistre le visage de l'utilisateur
     */
    @FXML
    void handleCapture(ActionEvent event) {
        try {
            if (userToRegister == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Utilisateur non défini.");
                return;
            }

            Mat frame = faceService.captureFrame();
            if (frame == null || frame.empty()) {
                showAlert(Alert.AlertType.WARNING, "Erreur", 
                    "Impossible de capturer l'image.");
                return;
            }

            // Détecter les visages
            MatOfRect faceDetections = faceService.detectFaces(frame);
            if (faceDetections.toArray().length == 0) {
                showAlert(Alert.AlertType.WARNING, "Erreur", 
                    "Aucun visage détecté.\n\nVeuillez repositionner votre visage.");
                return;
            }

            // Prendre le premier visage
            Rect faceRect = faceDetections.toArray()[0];
            Mat faceImage = faceService.extractFaceRegion(frame, faceRect);

            if (faceImage == null) {
                showAlert(Alert.AlertType.WARNING, "Erreur", 
                    "Erreur lors de l'extraction du visage.");
                return;
            }

            // Enregistrer le visage
            boolean registered = faceService.registerFace(userToRegister.getId(), faceImage);
            
            frame.release();
            faceImage.release();

            if (registered) {
                faceRegistered = true;
                statusLabel.setText("✅ Visage enregistré avec succès !");
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                    "Votre visage a été enregistré avec succès !\n\nPassage à la configuration de la 2FA...");
                
                // Naviguer vers Setup2FA après 2 secondes
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(this::navigateToSetup2FA);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Erreur lors de l'enregistrement du visage.");
                statusLabel.setText("❌ Erreur lors de l'enregistrement");
            }
        } catch (Exception e) {
            System.err.println("[FaceRegister] Erreur capture: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                "Erreur: " + e.getMessage());
        }
    }

    /**
     * Passe l'enregistrement du visage (revenir à l'écran précédent)
     */
    @FXML
    void handleSkip(ActionEvent event) {
        navigateBack();
    }

    /**
     * Revenir à l'écran de connexion
     */
    private void navigateBack() {
        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            if (faceService != null) {
                faceService.closeCamera();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/hospismart/hospismartdesktop/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[FaceRegister] Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Naviguer vers la configuration 2FA après enregistrement du visage
     */
    private void navigateToSetup2FA() {
        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            if (faceService != null) {
                faceService.closeCamera();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/hospismart/hospismartdesktop/Setup2FA.fxml"));
            Scene scene = new Scene(loader.load());
            SetupTwoFactorController controller = loader.getController();
            
            // Passer l'utilisateur au contrôleur Setup2FA
            controller.setNewUserRegistration(userToRegister);
            
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[FaceRegister] Erreur navigation vers Setup2FA: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la navigation: " + e.getMessage());
        }
    }

    /**
     * Convertir Mat OpenCV en Image JavaFX
     */
    private Image matToImage(Mat mat) {
        try {
            byte[] imageData = new byte[mat.channels() * mat.cols() * mat.rows()];
            mat.get(0, 0, imageData);

            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), imageData);

            return SwingFXUtils.toFXImage(image, null);
        } catch (Exception e) {
            System.err.println("[FaceRegister] Erreur conversion: " + e.getMessage());
            return null;
        }
    }

    /**
     * Affiche une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
