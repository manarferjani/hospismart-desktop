package com.hospismart.hospismartdesktop.tests;

import com.hospismart.hospismartdesktop.services.AIHealthSummaryService;
import com.hospismart.hospismartdesktop.services.HealthSummaryService;
import com.hospismart.hospismartdesktop.services.VoiceRecognitionService;

/**
 * Tests manuels pour la reconnaissance vocale
 * À exécuter depuis la ligne de commande ou un IDE
 */
public class VoiceRecognitionTest {

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  TEST SUITE - RECONNAISSANCE VOCALE                  ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        try {
            // Test 1: Vérifier la disponibilité de Vosk
            testVoskAvailability();
            
            // Test 2: Test du service IA
            testAIHealthSummary();
            
            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ TOUS LES TESTS PRÉLIMINAIRES RÉUSSIS           ║");
            System.out.println("╚════════════════════════════════════════════════════╝\n");
            System.out.println("ℹ️  Le test complet de reconnaissance vocale");
            System.out.println("    nécessite un microphone et l'interface GUI.\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR DURANT LES TESTS:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 1: Vérifie que Vosk est disponible et le modèle chargeable
     */
    private static void testVoskAvailability() {
        System.out.println("\n▶ TEST 1: Vérification Vosk et Modèle");
        System.out.println("─".repeat(50));
        
        boolean voskAvailable = VoiceRecognitionService.isVoskAvailable();
        
        if (voskAvailable) {
            System.out.println("✅ Vosk est disponible");
            System.out.println("✅ Modèle français trouvé");
            System.out.println("✅ Prêt pour reconnaissance vocale");
        } else {
            throw new RuntimeException(
                "❌ Vosk n'est pas disponible.\n" +
                "   Vérifiez:\n" +
                "   - Le dossier 'vosk-model-small-fr-0.22' existe\n" +
                "   - Il contient le dossier 'am/'\n" +
                "   - Les dépendances Vosk sont installées"
            );
        }
    }

    /**
     * Test 2: Teste le service IA avec du texte de test
     */
    private static void testAIHealthSummary() {
        System.out.println("\n▶ TEST 2: Service IA et Génération de Bilan");
        System.out.println("─".repeat(50));
        
        String testVoiceText = "Bonjour, j'ai mal à la tête depuis ce matin et je me sens très fatigué. " +
                              "J'ai aussi de la fièvre et une petite toux. Je suis préoccupé.";
        
        System.out.println("Texte de test: \"" + testVoiceText + "\"");
        System.out.println("\nGénération du bilan...\n");
        
        AIHealthSummaryService aiService = new AIHealthSummaryService();
        String summary = aiService.generateHealthSummary(testVoiceText);
        
        if (summary != null && !summary.isEmpty()) {
            System.out.println("✅ Bilan généré avec succès");
            System.out.println("─".repeat(50));
            System.out.println(summary);
            System.out.println("─".repeat(50));
        } else {
            throw new RuntimeException("❌ Impossible de générer le bilan");
        }
    }

    /**
     * Test 3: Teste la connectivité base de données
     * (Optionnel - peut être lancé seul)
     */
    public static void testDatabaseConnection() {
        System.out.println("\n▶ TEST 3: Connectivité Base de Données");
        System.out.println("─".repeat(50));
        
        try {
            HealthSummaryService healthService = new HealthSummaryService();
            System.out.println("✅ HealthSummaryService initialisé");
            System.out.println("✅ Table health_summaries créée/vérifiée");
            System.out.println("✅ Prêt pour sauvegarder les bilans");
        } catch (Exception e) {
            System.err.println("⚠️  Base de données non disponible");
            System.err.println("   (Ce n'est pas grave si MySQL n'est pas lancé lors des tests préliminaires)");
        }
    }
}
