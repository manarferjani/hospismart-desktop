package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.AIHealthSummaryService;
import com.hospismart.hospismartdesktop.services.HealthSummaryService;
import com.hospismart.hospismartdesktop.services.VoiceRecognitionService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour la consultation vocale et génération de bilan de santé par IA
 * Patients peuvent décrire leurs symptômes vocalement, l'IA génère un bilan
 */
public class VoiceHealthController {
    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea transcriptionArea;

    @FXML
    private TextArea summaryArea;

    private VoiceRecognitionService voiceService;
    private AIHealthSummaryService aiService;
    private HealthSummaryService healthService;
    private User currentUser;
    private Thread recordingThread;
    private boolean isRecording = false;
    private String currentVoiceText = "";

    @FXML
    public void initialize() {
        System.out.println("[VoiceHealth] ════════════════════════════════════════");
        System.out.println("[VoiceHealth] Initialisation du contrôleur vocal");
        System.out.println("[VoiceHealth] ════════════════════════════════════════");
        
        currentUser = Session.getInstance().getCurrentUser();
        System.out.println("[VoiceHealth] Patient: " + currentUser.getPrenom() + " " + currentUser.getNom());
        
        // Initialiser les services
        voiceService = new VoiceRecognitionService();
        aiService = new AIHealthSummaryService();
        healthService = new HealthSummaryService();
        
        // Vérifier si Vosk est disponible
        if (!VoiceRecognitionService.isVoskAvailable()) {
            statusLabel.setText("⚠️ Vosk non disponible - Installation/Configuration requise");
            startButton.setDisable(true);
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", 
                "Vosk n'est pas disponible.\n\n" +
                "Assurez-vous que:\n" +
                "• Le dossier 'vosk-model-small-fr-0.22' existe\n" +
                "• Le microphone est connecté et activé\n" +
                "• Les permissions audio sont accordées");
        } else {
            statusLabel.setText("✅ Prêt à enregistrer");
            startButton.setDisable(false);
        }
        
        transcriptionArea.setWrapText(true);
        summaryArea.setWrapText(true);
        
        System.out.println("[VoiceHealth] ✅ Contrôleur initialisé");
    }

    /**
     * Démarre l'enregistrement vocal
     */
    @FXML
    void handleStartRecording(ActionEvent event) {
        if (isRecording) {
            System.out.println("[VoiceHealth] Enregistrement déjà en cours");
            return;
        }

        isRecording = true;
        startButton.setDisable(true);
        stopButton.setDisable(false);
        transcriptionArea.clear();
        summaryArea.clear();
        currentVoiceText = "";
        
        statusLabel.setText("🔴 ENREGISTREMENT EN COURS... Parlez maintenant!");
        statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14;");

        System.out.println("[VoiceHealth] ════════════════════════════════════════");
        System.out.println("[VoiceHealth] Démarrage de l'enregistrement");
        System.out.println("[VoiceHealth] ════════════════════════════════════════");

        // Lancer l'enregistrement dans un thread séparé
        recordingThread = new Thread(() -> {
            try {
                String recognizedText = voiceService.startListening();

                Platform.runLater(() -> {
                    if (recognizedText != null && !recognizedText.isEmpty()) {
                        currentVoiceText = recognizedText;
                        transcriptionArea.setText(recognizedText);
                        statusLabel.setText("✅ Enregistrement terminé - Génération du bilan en cours...");
                        statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14;");
                        System.out.println("[VoiceHealth] Texte reconnu, génération du bilan...");
                        
                        // Générer le bilan avec l'IA
                        generateHealthSummary(recognizedText);
                    } else {
                        statusLabel.setText("❌ Aucun texte reconnu - Veuillez réessayer");
                        statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14;");
                        System.err.println("[VoiceHealth] Aucun texte reconnu");
                    }
                    
                    isRecording = false;
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                });
            } catch (Exception e) {
                System.err.println("[VoiceHealth] ❌ Erreur enregistrement: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14;");
                    isRecording = false;
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                });
            }
        });
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    /**
     * Arrête l'enregistrement vocal
     */
    @FXML
    void handleStopRecording(ActionEvent event) {
        System.out.println("[VoiceHealth] Arrêt demandé par l'utilisateur");
        voiceService.stopListening();
        stopButton.setDisable(true);
        statusLabel.setText("⏹️ Enregistrement arrêté - Traitement en cours...");
    }

    /**
     * Génère le bilan de santé avec l'IA
     */
    private void generateHealthSummary(String voiceText) {
        new Thread(() -> {
            try {
                System.out.println("[VoiceHealth] Appel au service IA pour générer le bilan...");
                String summary = aiService.generateHealthSummary(voiceText);
                
                Platform.runLater(() -> {
                    summaryArea.setText(summary);
                    statusLabel.setText("✅ Bilan généré avec succès - Vous pouvez le sauvegarder");
                    statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14;");
                    System.out.println("[VoiceHealth] ✅ Bilan généré et affiché");
                });
            } catch (Exception e) {
                System.err.println("[VoiceHealth] ❌ Erreur génération bilan: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    summaryArea.setText("❌ Erreur lors de la génération du bilan:\n\n" + e.getMessage());
                    statusLabel.setText("❌ Erreur génération");
                    statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14;");
                });
            }
        }).start();
    }

    /**
     * Sauvegarde le bilan de santé en base de données
     */
    @FXML
    void handleSaveSummary(ActionEvent event) {
        if (summaryArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Aucun bilan à sauvegarder. Générez d'abord un bilan.");
            return;
        }

        System.out.println("[VoiceHealth] Sauvegarde du bilan...");
        boolean success = healthService.saveHealthSummary(
            currentUser,
            currentVoiceText,
            summaryArea.getText()
        );
        
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", 
                "Bilan de santé sauvegardé avec succès!\n\n" +
                "Vous pourrez le consulter dans votre profil.");
            
            System.out.println("[VoiceHealth] ✅ Bilan sauvegardé pour: " + currentUser.getEmail());
            
            // Réinitialiser l'interface
            transcriptionArea.clear();
            summaryArea.clear();
            statusLabel.setText("✅ Nouveau bilan prêt à être enregistré");
            statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14;");
        } else {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", 
                "Impossible de sauvegarder le bilan.\n\n" +
                "Vérifiez votre connexion à la base de données.");
            System.err.println("[VoiceHealth] ❌ Erreur sauvegarde bilan");
        }
    }

    /**
     * Revient à la page précédente
     */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            System.out.println("[VoiceHealth] Retour au profil utilisateur");
            
            // Nettoyer les ressources
            if (isRecording) {
                voiceService.stopListening();
            }
            voiceService.cleanup();
            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.interrupt();
            }

            // Retourner au profil utilisateur
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/UserProfile.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[VoiceHealth] ❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur lors de la navigation: " + e.getMessage());
        }
    }

    /**
     * Affiche une alerte à l'utilisateur
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
