package com.hospismart.hospismartdesktop.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * Service pour la gestion de l'authentification à deux facteurs (2FA)
 * Utilise Google Authenticator / TOTP (Time-based One-Time Password)
 */
public class TwoFactorAuthService {
    private GoogleAuthenticator gAuth;
    private static final String QR_CODE_DIR = "qr_codes/";
    private static final int QR_CODE_SIZE = 300;

    public TwoFactorAuthService() {
        this.gAuth = new GoogleAuthenticator();
        // Créer le répertoire pour les QR codes s'il n'existe pas
        File dir = new File(QR_CODE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Génère une nouvelle clé secrète 2FA pour un utilisateur
     */
    public GoogleAuthenticatorKey generateSecret() {
        System.out.println("[2FA] Génération d'une nouvelle clé secrète");
        return gAuth.createCredentials();
    }

    /**
     * Génère l'URL de provisionnement pour le QR code
     * Format: otpauth://totp/[issuer]:[email]?secret=[SECRET]&issuer=[issuer]
     */
    public String generateQRCodeUrl(String userEmail, String secret, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            issuer,
            userEmail,
            secret,
            issuer
        );
    }

    /**
     * Génère une image de QR code et la sauvegarde
     */
    public String generateQRCodeImage(String userEmail, String secret, int userId) throws WriterException, IOException {
        String qrCodeUrl = generateQRCodeUrl(userEmail, secret, "HospiSmart");
        
        // Générer le QR code
        BitMatrix matrix = new MultiFormatWriter().encode(
            qrCodeUrl,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE
        );

        // Créer l'image
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
        
        // Sauvegarder l'image
        String fileName = QR_CODE_DIR + "user_" + userId + "_qrcode.png";
        File outputFile = new File(fileName);
        ImageIO.write(bufferedImage, "png", outputFile);
        
        System.out.println("[2FA] QR code généré et sauvegardé: " + fileName);
        return fileName;
    }

    /**
     * Génère une image de QR code en Base64 (pour affichage dans l'UI)
     */
    public String generateQRCodeBase64(String userEmail, String secret, int userId) throws WriterException, IOException {
        String qrCodeUrl = generateQRCodeUrl(userEmail, secret, "HospiSmart");
        
        // Générer le QR code
        BitMatrix matrix = new MultiFormatWriter().encode(
            qrCodeUrl,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE
        );

        // Créer l'image
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
        
        // Convertir en Base64
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageData = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageData);
        
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Vérifie un code TOTP
     */
    public boolean verifyCode(String secret, int code) {
        try {
            boolean isValid = gAuth.authorize(secret, code);
            if (isValid) {
                System.out.println("[2FA] ✅ Code TOTP vérifié avec succès");
            } else {
                System.out.println("[2FA] ❌ Code TOTP invalide");
            }
            return isValid;
        } catch (Exception e) {
            System.err.println("[2FA] ❌ Erreur lors de la vérification du code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère des codes de secours (backup codes) pour l'utilisateur
     * À utiliser en cas de perte du téléphone
     */
    public String[] generateBackupCodes(int count) {
        String[] backupCodes = new String[count];
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append((char) ('0' + (int) (Math.random() * 10)));
            }
            backupCodes[i] = code.toString();
        }
        System.out.println("[2FA] " + count + " codes de secours générés");
        return backupCodes;
    }

    /**
     * Obtient le code TOTP actuel pour un secret donné (utile pour les tests)
     */
    public int getCurrentCode(String secret) {
        return gAuth.getTotpPassword(secret);
    }

    /**
     * Génère le chemin du fichier QR code
     */
    public static String getQRCodeFilePath(int userId) {
        return QR_CODE_DIR + "user_" + userId + "_qrcode.png";
    }

    /**
     * Supprime le fichier QR code d'un utilisateur
     */
    public static void deleteQRCode(int userId) {
        try {
            File file = new File(getQRCodeFilePath(userId));
            if (file.exists() && file.delete()) {
                System.out.println("[2FA] QR code supprimé pour l'utilisateur " + userId);
            }
        } catch (Exception e) {
            System.err.println("[2FA] Erreur suppression QR code: " + e.getMessage());
        }
    }
}
