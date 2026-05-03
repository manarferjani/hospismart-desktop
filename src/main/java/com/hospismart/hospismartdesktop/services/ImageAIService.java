package com.hospismart.hospismartdesktop.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Service de génération d'images par Intelligence Artificielle.
 * Utilise l'API ouverte Pollinations.ai pour générer une image réaliste
 * d'un médicament à partir de son nom.
 */
public class ImageAIService {

    // Dossier local pour stocker les images générées
    private static final String IMAGE_DIR = "generated_images";

    /**
     * Génère une image IA pour un médicament donné.
     *
     * @param nomMedicament   Le nom du médicament (ex: "Amoxicilline")
     * @param categorie       La catégorie du médicament (ex: "Antibiotique"), peut être null
     * @param medicamentId    L'ID du médicament pour nommer le fichier
     * @return Le chemin absolu du fichier image généré, ou null en cas d'erreur.
     */
    public String genererImage(String nomMedicament, String categorie, int medicamentId) {
        try {
            // 1. Construire le prompt descriptif pour l'IA
            String prompt = construirePrompt(nomMedicament, categorie);
            System.out.println("🤖 Génération IA en cours pour : " + nomMedicament);

            // 2. Préparer l'URL d'appel (Pollinations.ai fonctionne en GET direct)
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString())
                .replace("+", "%20");
            // width=800, height=800, nologo=true (pour ne pas avoir le logo pollinations)
            String urlStr = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=800&height=800&nologo=true&enhance=true";

            URL url = new URL(urlStr);

            // 3. Télécharger l'image
            String filename = nomMedicament.toLowerCase()
                .replaceAll("[^a-z0-9]", "_") + "_" + medicamentId + ".jpg";

            Path dirPath = Paths.get(IMAGE_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(filename);

            // On ouvre le stream et on copie directement dans le fichier
            try (InputStream in = url.openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("✅ Image IA générée : " + filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            System.err.println("❌ Erreur génération image IA : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Construit un prompt optimisé pour générer une image de médicament réaliste avec le texte.
     * Chaque appel produit un design différent grâce à la randomisation du style.
     *
     * @param nom       Nom du médicament
     * @param categorie Catégorie (peut être null)
     * @return Le prompt textuel
     */
    private String construirePrompt(String nom, String categorie) {
        // Styles et couleurs aléatoires pour varier les designs
        String[] boxStyles = {
            "sleek rectangular cardboard box",
            "modern rounded medicine bottle with cap",
            "elegant blister pack with cardboard backing",
            "premium hexagonal medicine box",
            "minimalist cylindrical medicine container",
            "luxury matte-finish pharmaceutical box"
        };
        String[] colors = {
            "deep blue and white", "emerald green and gold", "royal purple and silver",
            "coral red and cream", "teal and white", "navy and orange",
            "dark green and beige", "burgundy and gold", "sky blue and white"
        };

        java.util.Random rand = new java.util.Random();
        String style = boxStyles[rand.nextInt(boxStyles.length)];
        String color = colors[rand.nextInt(colors.length)];

        StringBuilder prompt = new StringBuilder();
        prompt.append("A ").append(color).append(" ").append(style);
        prompt.append(". The front label has the word \"").append(nom).append("\" ");
        prompt.append("printed in very large, bold, clearly readable uppercase letters at the center. ");
        if (categorie != null && !categorie.isEmpty()) {
            prompt.append("Below in smaller text it says \"").append(categorie).append("\". ");
        }
        prompt.append("Photorealistic pharmaceutical product photography, studio lighting, solid white background, sharp focus, 8k");
        return prompt.toString();
    }
}
