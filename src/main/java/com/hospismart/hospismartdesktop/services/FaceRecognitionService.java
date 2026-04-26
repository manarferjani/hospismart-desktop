package com.hospismart.hospismartdesktop.services;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FaceRecognitionService {
    static {
        nu.pattern.OpenCV.loadShared();
    }

    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private Map<Integer, Mat> registeredFaces = new HashMap<>();
    private String facesDataPath = "faces_data/";

    public FaceRecognitionService() {
        // Charger le classificateur en cascade pour la détection de visages
        try {
            faceDetector = new CascadeClassifier();
            // Essayer plusieurs chemins possibles
            String[] possiblePaths = {
                "src/main/resources/haarcascade_frontalface_alt.xml",
                "target/classes/haarcascade_frontalface_alt.xml",
                "haarcascade_frontalface_alt.xml",
                System.getProperty("user.dir") + "/src/main/resources/haarcascade_frontalface_alt.xml",
                System.getProperty("user.dir") + "/target/classes/haarcascade_frontalface_alt.xml"
            };
            
            boolean loaded = false;
            for (String path : possiblePaths) {
                File cascadeFile = new File(path);
                if (cascadeFile.exists() && faceDetector.load(path)) {
                    System.out.println("[FaceRecognition] Classificateur chargé de: " + path);
                    loaded = true;
                    break;
                }
            }
            
            if (!loaded) {
                System.err.println("[FaceRecognition] ⚠️ ATTENTION: Impossible de charger le classificateur Haar Cascade");
                System.err.println("[FaceRecognition] Chemins testés:");
                for (String path : possiblePaths) {
                    System.err.println("   - " + new File(path).getAbsolutePath() + " (existe: " + new File(path).exists() + ")");
                }
                System.err.println("[FaceRecognition] La détection faciale ne fonctionnera pas");
                System.err.println("[FaceRecognition] Assurez-vous que 'haarcascade_frontalface_alt.xml' est présent dans src/main/resources/");
            }
            
            // Créer le répertoire pour stocker les données faciales
            File dataDir = new File(facesDataPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialise la caméra
     */
    public boolean initializeCamera() {
        try {
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("[FaceRecognition] ERREUR: Caméra non accessible");
                return false;
            }
            System.out.println("[FaceRecognition] Caméra initialisée avec succès");
            return true;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR initialisation caméra: " + e.getMessage());
            return false;
        }
    }

    /**
     * Capture une frame depuis la caméra
     */
    public Mat captureFrame() {
        if (camera == null || !camera.isOpened()) {
            return null;
        }
        Mat frame = new Mat();
        camera.read(frame);
        return frame;
    }

    /**
     * Détecte les visages dans une image
     */
    public MatOfRect detectFaces(Mat image) {
        if (faceDetector == null || faceDetector.empty()) {
            System.err.println("[FaceRecognition] Classificateur non chargé");
            return new MatOfRect();
        }
        
        MatOfRect faceDetections = new MatOfRect();
        Mat grayImage = new Mat();
        
        // Convertir en image en niveaux de gris
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        
        // Détecter les visages
        faceDetector.detectMultiScale(grayImage, faceDetections, 1.1, 4, 0, 
            new Size(30, 30), new Size(300, 300));
        
        return faceDetections;
    }

    /**
     * Enregistre un visage pour un utilisateur
     */
    public boolean registerFace(int userId, Mat faceImage) {
        try {
            String fileName = facesDataPath + "user_" + userId + ".xml";
            
            // Sauvegarder le visage localement
            MatOfInt compressionParams = new MatOfInt(
                org.opencv.imgcodecs.Imgcodecs.IMWRITE_PNG_COMPRESSION, 9
            );
            
            // Convertir en PNG pour stocker
            return org.opencv.imgcodecs.Imgcodecs.imwrite(
                facesDataPath + "user_" + userId + ".png", 
                faceImage
            );
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR enregistrement visage: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si un visage correspond à un utilisateur enregistré
     */
    public boolean verifyFace(int userId, Mat capturedFace) {
        try {
            String savedFacePath = facesDataPath + "user_" + userId + ".png";
            File savedFaceFile = new File(savedFacePath);
            
            if (!savedFaceFile.exists()) {
                System.out.println("[FaceRecognition] Aucun visage enregistré pour l'utilisateur " + userId);
                return false;
            }

            // Charger l'image du visage enregistré
            Mat savedFace = org.opencv.imgcodecs.Imgcodecs.imread(savedFacePath);
            
            if (savedFace.empty()) {
                System.err.println("[FaceRecognition] Erreur chargement visage enregistré");
                return false;
            }

            // Redimensionner les two images pour les rendre comparables
            Mat resizedCaptured = new Mat();
            Mat resizedSaved = new Mat();
            
            Imgproc.resize(capturedFace, resizedCaptured, new Size(200, 200));
            Imgproc.resize(savedFace, resizedSaved, new Size(200, 200));

            // Comparer les histogrammes
            double similarity = compareFacesUsingHistogram(resizedCaptured, resizedSaved);
            
            System.out.println("[FaceRecognition] Similarité: " + String.format("%.2f%%", similarity * 100));
            
            // Si similarité > 60% c'est un match
            boolean isMatch = similarity > 0.60;
            
            resizedCaptured.release();
            resizedSaved.release();
            savedFace.release();
            
            return isMatch;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR vérification visage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Aide-fonction : comparer deux visages via histogramme
     */
    private double compareFacesUsingHistogram(Mat face1, Mat face2) {
        // Convertir en niveaux de gris
        Mat gray1 = new Mat();
        Mat gray2 = new Mat();
        Imgproc.cvtColor(face1, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(face2, gray2, Imgproc.COLOR_BGR2GRAY);

        // Calculer les histogrammes
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0, 256);
        MatOfInt channels = new MatOfInt(0);
        
        Imgproc.calcHist(java.util.Arrays.asList(gray1), channels, new Mat(), 
            hist1, histSize, ranges);
        Imgproc.calcHist(java.util.Arrays.asList(gray2), channels, new Mat(), 
            hist2, histSize, ranges);

        // Normaliser
        Core.normalize(hist1, hist1, 1, 0, Core.NORM_L1);
        Core.normalize(hist2, hist2, 1, 0, Core.NORM_L1);

        // Comparer les histogrammes
        double similarity = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
        
        gray1.release();
        gray2.release();
        hist1.release();
        hist2.release();
        
        return Math.max(0, Math.min(1, (similarity + 1) / 2)); // Normaliser entre 0 et 1
    }

    /**
     * Extrait la région du visage détecté
     */
    public Mat extractFaceRegion(Mat image, Rect faceRect) {
        try {
            Mat faceRegion = new Mat(image, faceRect);
            Mat extractedFace = new Mat();
            faceRegion.copyTo(extractedFace);
            return extractedFace;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR extraction région visage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Ferme la caméra
     */
    public void closeCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            System.out.println("[FaceRecognition] Caméra fermée");
        }
    }

    /**
     * Vérifie si une caméra est disponible
     */
    public static boolean isCameraAvailable() {
        try {
            VideoCapture cap = new VideoCapture(0);
            boolean available = cap.isOpened();
            cap.release();
            return available;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] ERREUR vérification caméra: " + e.getMessage());
            return false;
        }
    }

    /**
     * Dessine les rectangles de détection sur l'image
     */
    public void drawFaceDetections(Mat image, MatOfRect faceDetections) {
        Rect[] faces = faceDetections.toArray();
        for (Rect face : faces) {
            Imgproc.rectangle(image, 
                new Point(face.x, face.y), 
                new Point(face.x + face.width, face.y + face.height),
                new Scalar(0, 255, 0), 2);
        }
    }
}
