package com.hospismart.hospismartdesktop.services;

import javax.sound.sampled.*;
import java.io.File;
import org.vosk.Model;
import org.vosk.Recognizer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Service de reconnaissance vocale avec intégration Vosk
 * Utilise le modèle Vosk extrait dans le dossier "model"
 * Capture audio du microphone et traitement en temps réel
 */
public class VoiceRecognitionService {
    
    private boolean isListening = false;
    private String recognizedText = "";
    private TargetDataLine currentLine;
    private Model voskModel;
    private static final long RECORDING_TIMEOUT = 10000; // 10 secondes max
    private static final String MODEL_PATH = "model"; // Chemin du modèle Vosk extrait

    public VoiceRecognitionService() {
        try {
            System.out.println("[Voice] ════════════════════════════════════════");
            System.out.println("[Voice] Initialisation Voice Recognition Service");
            System.out.println("[Voice] Modèle: " + MODEL_PATH);
            System.out.println("[Voice] ════════════════════════════════════════");
            
            if (isVoskAvailable()) {
                System.out.println("[Voice] ✅ Modèle Vosk détecté et prêt");
                voskModel = new Model(MODEL_PATH);
            } else {
                System.out.println("[Voice] ⚠️ Modèle Vosk non trouvé - Mode simulation");
            }
            
            System.out.println("[Voice] ✅ Service initialisé");
            System.out.println("[Voice] ════════════════════════════════════════");
        } catch (Exception e) {
            System.err.println("[Voice] Erreur initialisation: " + e.getMessage());
        }
    }

    /**
     * Vérifie si le modèle Vosk est disponible
     */
    public static boolean isVoskAvailable() {
        try {
            System.out.println("[Voice] Vérification du modèle Vosk...");
            
            File modelDir = new File(MODEL_PATH);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                System.out.println("[Voice] ⚠️ Dossier modèle non trouvé: " + modelDir.getAbsolutePath());
                return false;
            }
            
            // Vérifier la structure du modèle
            File amFolder = new File(modelDir, "am");
            if (!amFolder.exists() || !amFolder.isDirectory()) {
                System.out.println("[Voice] ⚠️ Structure modèle invalide (pas de dossier 'am')");
                return false;
            }
            
            // Vérifier les fichiers critiques
            File finalMdl = new File(amFolder, "final.mdl");
            if (!finalMdl.exists()) {
                System.out.println("[Voice] ⚠️ Fichier modèle 'final.mdl' manquant");
                return false;
            }
            
            System.out.println("[Voice] ✅ Modèle Vosk trouvé: " + modelDir.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            System.err.println("[Voice] ❌ Erreur vérification: " + e.getMessage());
            return false;
        }
    }

    /**
     * Démarre l'enregistrement vocal et reconnaissance
     * Utilise Vosk si disponible, sinon simulation
     */
    public String startListening() {
        try {
            isListening = true;
            recognizedText = "";
            
            System.out.println("[Voice] 🎤 Démarrage de l'enregistrement");
            
            // Vérifier si Vosk est disponible
            if (isVoskAvailable()) {
                System.out.println("[Voice] Tentative utilisation de Vosk...");
                String result = captureAudioWithVosk();
                if (result != null) {
                    recognizedText = result;
                    System.out.println("[Voice] ✅ Reconnaissance Vosk terminée");
                    return result;
                }
            }
            
            System.out.println("[Voice] ⚠️ Vosk non disponible ou a échoué.");
            return "";
            
        } catch (Exception e) {
            System.err.println("[Voice] ❌ Erreur: " + e.getMessage());
            return "";
        }
    }

    /**
     * Capture audio du microphone
     */
    private String captureAudioWithVosk() {
        if (voskModel == null) {
            System.out.println("[Voice] ⚠️ Modèle Vosk non chargé");
            return null;
        }
        try (Recognizer recognizer = new Recognizer(voskModel, 16000)) {
            System.out.println("[Voice] Activation du microphone...");
            
            // Format audio: 16kHz, 16-bit, mono, signed
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[Voice] ❌ Format audio non supporté par le système");
                return null;
            }
            
            currentLine = (TargetDataLine) AudioSystem.getLine(info);
            currentLine.open(format);
            currentLine.start();
            
            System.out.println("[Voice] ✅ Microphone activé");
            System.out.println("[Voice] 🎤 Parlez maintenant (max 10 secondes)...");
            
            // Enregistrer pendant 10 secondes max
            byte[] buffer = new byte[4096];
            long startTime = System.currentTimeMillis();
            
            while (isListening && System.currentTimeMillis() - startTime < RECORDING_TIMEOUT) {
                try {
                    int bytesRead = currentLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            // Optionnel: afficher les résultats au fur et à mesure
                            // System.out.println("[Voice] " + extractText(recognizer.getResult()));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Voice] ⚠️ Erreur lecture audio: " + e.getMessage());
                }
            }
            
            System.out.println("[Voice] ✅ Enregistrement terminé");
            closeAudioLine();
            
            String finalResult = extractText(recognizer.getFinalResult());
            return finalResult;
            
        } catch (LineUnavailableException e) {
            System.err.println("[Voice] ❌ Microphone non disponible: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[Voice] ❌ Erreur capture audio: " + e.getMessage());
            return null;
        } finally {
            closeAudioLine();
        }
    }

    /**
     * Arrête l'enregistrement
     */
    public void stopListening() {
        System.out.println("[Voice] ⏹️ Arrêt de l'enregistrement demandé");
        isListening = false;
        closeAudioLine();
    }

    /**
     * Ferme la ligne audio de manière sécurisée
     */
    private synchronized void closeAudioLine() {
        TargetDataLine line = currentLine;
        if (line != null) {
            try {
                currentLine = null;
                line.stop();
                line.close();
                System.out.println("[Voice] ✅ Microphone fermé");
            } catch (Exception e) {
                System.err.println("[Voice] ⚠️ Erreur fermeture microphone: " + e.getMessage());
            }
        }
    }



    /**
     * Libère les ressources
     */
    public void cleanup() {
        System.out.println("[Voice] 🧹 Nettoyage des ressources...");
        isListening = false;
        closeAudioLine();
        if (voskModel != null) {
            try {
                voskModel.close();
                voskModel = null;
            } catch (Exception e) {
                System.err.println("[Voice] ⚠️ Erreur fermeture modèle: " + e.getMessage());
            }
        }
        System.out.println("[Voice] ✅ Service nettoyé");
    }



    /**
     * Extrait le texte du résultat JSON de Vosk
     * Formats supportés:
     *  - {"result":[{"conf":1.0,"end":1.23,"start":0.56,"word":"bonjour"},...]}
     *  - {"partial":"bon..."}
     *  - {}
     */
    private String extractText(String jsonResult) {
        try {
            if (jsonResult == null || jsonResult.isEmpty() || jsonResult.equals("{}")) {
                return "";
            }
            
            // Correction de l'encodage (Vosk renvoie de l'UTF-8 mais JNA utilise parfois l'encodage système sous Windows)
            if (!java.nio.charset.Charset.defaultCharset().equals(java.nio.charset.StandardCharsets.UTF_8)) {
                byte[] bytes = jsonResult.getBytes(java.nio.charset.Charset.defaultCharset());
                jsonResult = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            JsonObject jsonObject = JsonParser.parseString(jsonResult).getAsJsonObject();
            String extracted = "";
            
            if (jsonObject.has("text")) {
                extracted = jsonObject.get("text").getAsString().trim();
            } else if (jsonObject.has("partial")) {
                extracted = jsonObject.get("partial").getAsString().trim();
            }
            
            if (!extracted.isEmpty()) {
                System.out.println("[Voice] ✓ Texte reconnu: \"" + extracted + "\"");
            }
            return extracted;
            
        } catch (Exception e) {
            System.err.println("[Voice] ❌ Erreur extraction texte: " + e.getMessage());
            return "";
        }
    }
}
