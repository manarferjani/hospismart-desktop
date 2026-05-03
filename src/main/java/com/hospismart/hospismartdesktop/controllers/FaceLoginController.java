package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.FaceRecognitionService;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.animation.AnimationTimer;
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
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class FaceLoginController {
    @FXML
    private ImageView cameraView;

    @FXML
    private Label statusLabel;

    @FXML
    private Button captureButton;

    @FXML
    private Button cancelButton;

    private FaceRecognitionService faceService;
    private UserService userService;
    private AnimationTimer animationTimer;
    private boolean isCapturing = false;
    private User detectedUser = null;

    @FXML
    public void initialize() {
        try {
            this.faceService = new FaceRecognitionService();
            this.userService = new UserService();

            // Vérifier que la caméra est disponible
            if (!FaceRecognitionService.isCameraAvailable()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Aucune caméra détectée.\n\nVeuillez brancher une caméra et réessayer.");
                statusLabel.setText("❌ Caméra non disponible");
                captureButton.setDisable(true);
                return;
            }

            // Initialiser la caméra
            if (!faceService.initializeCamera()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'accéder à la caméra.");
                statusLabel.setText("❌ Erreur d'accès caméra");
                captureButton.setDisable(true);
                return;
            }

            statusLabel.setText("✅ Caméra initialisée - Positionnez votre visage");
            startCameraStream();
        } catch (Exception e) {
            System.err.println("[FaceLogin] Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Erreur lors de l'initialisation: " + e.getMessage());
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

                        // Dessiner les rectangles autour des visages détectés
                        faceService.drawFaceDetections(frame, faceDetections);

                        if (faceDetections.toArray().length > 0) {
                            statusLabel.setText("👤 " + faceDetections.toArray().length + " visage(s) détecté(s)");
                        } else {
                            statusLabel.setText("❌ Aucun visage détecté");
                        }

                        // Afficher le flux vidéo
                        Image image = matToImage(frame);
                        if (image != null) {
                            cameraView.setImage(image);
                        }

                        frame.release();
                    }
                } catch (Exception e) {
                    System.err.println("[FaceLogin] Erreur flux vidéo: " + e.getMessage());
                }
            }
        };
        animationTimer.start();
    }

    /**
     * Capture le visage pour authentification
     */
    @FXML
    void handleCapture(ActionEvent event) {
        try {
            Mat frame = faceService.captureFrame();
            if (frame == null || frame.empty()) {
                showAlert(Alert.AlertType.WARNING, "Erreur",
                    "Impossible de capturer l'image.");
                return;
            }

            // Détecter les visages dans le cadre
            MatOfRect faceDetections = faceService.detectFaces(frame);
            if (faceDetections.toArray().length == 0) {
                showAlert(Alert.AlertType.WARNING, "Erreur",
                    "Aucun visage détecté.\n\nVeuillez repositionner votre visage et réessayer.");
                return;
            }

            // Prendre le premier visage détecté
            Rect faceRect = faceDetections.toArray()[0];
            Mat faceImage = faceService.extractFaceRegion(frame, faceRect);

            if (faceImage == null) {
                showAlert(Alert.AlertType.WARNING, "Erreur",
                    "Erreur lors de l'extraction du visage.");
                return;
            }

            isCapturing = true;
            statusLabel.setText("🔄 Vérification du visage...");
            captureButton.setDisable(true);

            // Arrêter le flux vidéo
            if (animationTimer != null) {
                animationTimer.stop();
            }

            // Vérifier le visage contre les utilisateurs enregistrés
            verifyFaceAgainstDatabase(faceImage);

            frame.release();
            faceImage.release();
        } catch (Exception e) {
            System.err.println("[FaceLogin] Erreur capture: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Erreur lors de la capture: " + e.getMessage());
            isCapturing = false;
            captureButton.setDisable(false);
            if (animationTimer != null) {
                animationTimer.start();
            }
        }
    }

    /**
     * Vérifie le visage capturé contre tous les utilisateurs enregistrés en base
     */
    private void verifyFaceAgainstDatabase(Mat capturedFace) {
        try {
            List<User> allUsers = userService.afficher();

            for (User user : allUsers) {
                if (user.isActive() && faceService.verifyFace(user.getId(), capturedFace)) {
                    // Visage reconnu !
                    detectedUser = user;
                    Session.getInstance().setCurrentUser(user);

                    statusLabel.setText("✅ Bienvenue " + user.getPrenom() + " " + user.getNom() + "!");

                    // Naviguer vers le tableau de bord approprié
                    navigateToUserDashboard(user);
                    return;
                }
            }

            // Aucun visage reconnu
            showAlert(Alert.AlertType.WARNING, "Authentification échouée",
                "Visage non reconnu.\n\nVeuillez vous enregistrer d'abord ou réessayer.");
            statusLabel.setText("❌ Visage non reconnu");

            isCapturing = false;
            captureButton.setDisable(false);
            startCameraStream();
        } catch (Exception e) {
            System.err.println("[FaceLogin] Erreur vérification: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Erreur lors de la vérification: " + e.getMessage());
            isCapturing = false;
            captureButton.setDisable(false);
            startCameraStream();
        }
    }

    /**
     * Navigue vers le tableau de bord de l'utilisateur
     */
    private void navigateToUserDashboard(User user) {
        try {
            com.hospismart.hospismartdesktop.main.JavaFxMain.showDashboard(user);
        } catch (Exception e) {
            System.err.println("[FaceLogin] Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Revenir à l'écran de connexion classique
     */
    @FXML
    void handleCancel(ActionEvent event) {
        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            faceService.closeCamera();

            com.hospismart.hospismartdesktop.main.JavaFxMain.setRoot("/com/hospismart/hospismartdesktop/Login.fxml", null);
        } catch (Exception e) {
            System.err.println("[FaceLogin] Erreur retour: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("[FaceLogin] Erreur conversion Mat->Image: " + e.getMessage());
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

    /**
     * Nettoie les ressources à la fermeture
     */
    @FXML
    void onWindowClosed() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (faceService != null) {
            faceService.closeCamera();
        }
    }
}
