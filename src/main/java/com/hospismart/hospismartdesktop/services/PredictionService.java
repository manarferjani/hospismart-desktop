package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.MouvementStock;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Service de prédiction de rupture de stock basé sur l'Intelligence Artificielle.
 * Combine une analyse mathématique des mouvements historiques avec l'API Pollinations AI
 * pour générer un rapport de prédiction intelligent et actionnable.
 */
public class PredictionService {

    // API IA gratuite pour la génération de texte (pas de clé requise)
    private static final String AI_TEXT_API = "https://text.pollinations.ai/";

    /**
     * Résultat d'une prédiction pour un médicament.
     */
    public static class PredictionResult {
        public String nomMedicament;
        public int stockActuel;
        public int seuilAlerte;
        public double consommationJour;      // unités/jour moyenne
        public int joursAvantRupture;        // estimation en jours (-1 si pas de données)
        public String dateRuptureEstimee;    // date estimée de rupture
        public String niveauRisque;          // CRITIQUE / ÉLEVÉ / MODÉRÉ / FAIBLE
        public String couleurRisque;         // code couleur hex
        public int totalSorties;             // total des sorties sur la période
        public int totalEntrees;             // total des entrées sur la période
        public int nbMouvements;             // nombre total de mouvements analysés

        @Override
        public String toString() {
            return String.format("%s | Stock: %d | Conso: %.1f/j | Rupture: %s | Risque: %s",
                nomMedicament, stockActuel, consommationJour,
                joursAvantRupture >= 0 ? joursAvantRupture + " jours" : "N/A",
                niveauRisque);
        }
    }

    /**
     * Calcule la prédiction de rupture pour un médicament donné.
     *
     * @param med         Le médicament à analyser
     * @param mouvements  La liste de ses mouvements de stock
     * @return Un objet PredictionResult contenant l'analyse complète
     */
    public PredictionResult predire(Medicament med, List<MouvementStock> mouvements) {
        PredictionResult result = new PredictionResult();
        result.nomMedicament = med.getNom();
        result.stockActuel = med.getQuantite();
        result.seuilAlerte = med.getSeuilAlerte();

        // Filtrer les sorties et entrées
        List<MouvementStock> sorties = mouvements.stream()
            .filter(m -> "SORTIE".equalsIgnoreCase(m.getType()))
            .collect(Collectors.toList());
        List<MouvementStock> entrees = mouvements.stream()
            .filter(m -> "ENTREE".equalsIgnoreCase(m.getType()))
            .collect(Collectors.toList());

        result.totalSorties = sorties.stream().mapToInt(MouvementStock::getQuantite).sum();
        result.totalEntrees = entrees.stream().mapToInt(MouvementStock::getQuantite).sum();
        result.nbMouvements = mouvements.size();

        if (sorties.size() < 2) {
            // Pas assez de données pour prédire
            result.consommationJour = 0;
            result.joursAvantRupture = -1;
            result.dateRuptureEstimee = "Données insuffisantes";
            result.niveauRisque = med.getQuantite() <= med.getSeuilAlerte() ? "ÉLEVÉ" : "FAIBLE";
            result.couleurRisque = med.getQuantite() <= med.getSeuilAlerte() ? "#dc3545" : "#198754";
            return result;
        }

        // Calculer la période d'analyse (du premier au dernier mouvement)
        LocalDateTime premierMouvement = sorties.stream()
            .map(MouvementStock::getDateMouvement)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        LocalDateTime dernierMouvement = sorties.stream()
            .map(MouvementStock::getDateMouvement)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());

        long joursAnalyse = ChronoUnit.DAYS.between(premierMouvement, dernierMouvement);
        if (joursAnalyse <= 0) joursAnalyse = 1;

        // Consommation moyenne par jour
        result.consommationJour = (double) result.totalSorties / joursAnalyse;

        // Prédiction : combien de jours avant rupture (stock = 0)
        if (result.consommationJour > 0) {
            result.joursAvantRupture = (int) Math.ceil(result.stockActuel / result.consommationJour);
            LocalDateTime dateRupture = LocalDateTime.now().plusDays(result.joursAvantRupture);
            result.dateRuptureEstimee = dateRupture.toLocalDate().toString();
        } else {
            result.joursAvantRupture = -1;
            result.dateRuptureEstimee = "Consommation nulle";
        }

        // Niveau de risque
        if (result.stockActuel == 0) {
            result.niveauRisque = "🔴 RUPTURE TOTALE";
            result.couleurRisque = "#dc3545";
        } else if (result.joursAvantRupture >= 0 && result.joursAvantRupture <= 7) {
            result.niveauRisque = "🔴 CRITIQUE";
            result.couleurRisque = "#dc3545";
        } else if (result.joursAvantRupture >= 0 && result.joursAvantRupture <= 15) {
            result.niveauRisque = "🟠 ÉLEVÉ";
            result.couleurRisque = "#fd7e14";
        } else if (result.joursAvantRupture >= 0 && result.joursAvantRupture <= 30) {
            result.niveauRisque = "🟡 MODÉRÉ";
            result.couleurRisque = "#ffc107";
        } else {
            result.niveauRisque = "🟢 FAIBLE";
            result.couleurRisque = "#198754";
        }

        return result;
    }

    /**
     * Génère un rapport d'analyse IA à partir des prédictions calculées.
     * Appelle l'API Pollinations AI pour produire un texte intelligent.
     *
     * @param predictions Liste des prédictions pour tous les médicaments
     * @return Le texte du rapport IA, ou null en cas d'erreur.
     */
    public String genererRapportIA(List<PredictionResult> predictions) {
        try {
            // Construire le résumé des données pour l'IA
            StringBuilder donnees = new StringBuilder();
            donnees.append("Tu es un expert en gestion pharmaceutique hospitalière. ");
            donnees.append("Analyse ces données de stock et donne un rapport concis en français avec des recommandations. ");
            donnees.append("Utilise des emojis. Sois professionnel mais clair.\n\n");
            donnees.append("DONNÉES DE STOCK HOSPISMART :\n");

            for (PredictionResult p : predictions) {
                donnees.append(String.format("- %s : Stock=%d, Seuil=%d, Conso=%.1f/jour, ",
                    p.nomMedicament, p.stockActuel, p.seuilAlerte, p.consommationJour));
                if (p.joursAvantRupture >= 0) {
                    donnees.append(String.format("Rupture estimée dans %d jours (%s), ", p.joursAvantRupture, p.dateRuptureEstimee));
                }
                donnees.append("Risque: " + p.niveauRisque + "\n");
            }

            donnees.append("\nDonne un rapport avec : 1) Résumé global 2) Médicaments critiques à commander en urgence ");
            donnees.append("3) Recommandations d'achat 4) Prévision pour les 30 prochains jours. Maximum 300 mots.");

            // Appel à l'API IA
            String encodedPrompt = URLEncoder.encode(donnees.toString(), StandardCharsets.UTF_8.toString())
                .replace("+", "%20");
            String urlStr = AI_TEXT_API + encodedPrompt;

            System.out.println("🧠 Appel API IA pour le rapport de prédiction...");

            URL url = new URL(urlStr);
            try (InputStream in = url.openStream();
                 Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                String rapport = scanner.hasNext() ? scanner.next() : "";
                System.out.println("✅ Rapport IA généré !");
                return rapport;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur génération rapport IA : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
