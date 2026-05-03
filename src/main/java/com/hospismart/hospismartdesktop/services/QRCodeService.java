package com.hospismart.hospismartdesktop.services;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Service de génération de QR Codes via l'API gratuite goqr.me.
 * Génère un QR Code contenant les informations détaillées d'un médicament
 * (nom, quantité, prix, date de péremption, catégorie, statut du stock).
 */
public class QRCodeService {

    // API gratuite, sans clé requise
    private static final String API_URL = "https://api.qrserver.com/v1/create-qr-code/";
    private static final String QR_DIR = "generated_qrcodes";

    /**
     * Génère un QR Code contenant les informations d'un médicament.
     *
     * @param nom          Nom du médicament
     * @param quantite     Quantité en stock
     * @param seuilAlerte  Seuil d'alerte
     * @param prixUnitaire Prix unitaire
     * @param datePeremption Date de péremption (peut être null)
     * @param categorie    Catégorie du médicament (peut être null)
     * @param medicamentId ID du médicament
     * @return Le chemin absolu du fichier QR Code généré, ou null en cas d'erreur.
     */
    public String genererQRCode(String nom, int quantite, int seuilAlerte,
                                double prixUnitaire, String datePeremption,
                                String categorie, int medicamentId) {
        try {
            // 1. Construire le contenu du QR Code
            String statut = quantite <= seuilAlerte ? "⚠️ ALERTE STOCK" : "✅ Stock OK";
            StringBuilder contenu = new StringBuilder();
            contenu.append("═══ HOSPISMART ═══\n");
            contenu.append("💊 ").append(nom).append("\n");
            if (categorie != null && !categorie.isEmpty()) {
                contenu.append("📂 Catégorie: ").append(categorie).append("\n");
            }
            contenu.append("📦 Quantité: ").append(quantite).append("\n");
            contenu.append("⚠️ Seuil: ").append(seuilAlerte).append("\n");
            contenu.append(String.format("💰 Prix: %.2f TND\n", prixUnitaire));
            if (datePeremption != null && !datePeremption.isEmpty()) {
                contenu.append("📅 Péremption: ").append(datePeremption).append("\n");
            }
            contenu.append("📊 Statut: ").append(statut).append("\n");
            contenu.append("═══════════════════");

            System.out.println("📱 Génération QR Code pour : " + nom);

            // 2. Encoder le contenu et construire l'URL de l'API
            String encodedData = URLEncoder.encode(contenu.toString(), StandardCharsets.UTF_8.toString());
            String urlStr = API_URL + "?size=400x400&data=" + encodedData + "&format=png&margin=10";

            URL url = new URL(urlStr);

            // 3. Télécharger l'image QR Code
            String filename = "qr_" + nom.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_" + medicamentId + ".png";

            Path dirPath = Paths.get(QR_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(filename);

            try (InputStream in = url.openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("✅ QR Code généré : " + filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
