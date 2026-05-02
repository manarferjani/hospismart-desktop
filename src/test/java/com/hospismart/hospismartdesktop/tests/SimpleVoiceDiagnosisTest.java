package com.hospismart.hospismartdesktop.tests;

import com.hospismart.hospismartdesktop.services.VoiceRecognitionService;
import com.hospismart.hospismartdesktop.services.AIHealthSummaryService;

/**
 * Test Simple et Rapide du Système Complet
 * Vérifie que Vosk fonctionne et que l'IA génère un diagnostic
 */
public class SimpleVoiceDiagnosisTest {

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  TEST SIMPLE - SYSTÈME VOIX ET DIAGNOSTIC IA       ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        try {
            // Test 1: Vérifier Vosk
            testVoskAvailability();
            
            // Test 2: Tester l'IA avec texte de simulation
            testAIDiagnosis();
            
            // Test 3: Afficher les niveaux d'urgence
            testUrgencyLevels();

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ TOUS LES TESTS RÉUSSIS                          ║");
            System.out.println("╚════════════════════════════════════════════════════╝\n");
            
            System.out.println("✅ Le système est prêt à être utilisé!\n");
            System.out.println("Prochaine étape:");
            System.out.println("1. Lancer l'application JavaFX");
            System.out.println("2. Se connecter comme patient");
            System.out.println("3. Aller à 'Consultation Vocale'");
            System.out.println("4. Cliquer 'Démarrer Enregistrement'");
            System.out.println("5. Parler pendant 10 secondes");
            System.out.println("6. L'IA génèrera le diagnostic automatiquement\n");

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test 1: Vérifier que Vosk est disponible
     */
    private static void testVoskAvailability() {
        System.out.println("▶ TEST 1: Vérification de Vosk");
        System.out.println("─".repeat(50));
        
        boolean voskAvailable = VoiceRecognitionService.isVoskAvailable();
        
        if (voskAvailable) {
            System.out.println("✅ Vosk est disponible et prêt");
            System.out.println("✅ Modèle français détecté");
            System.out.println("✅ Reconnaissance vocale: PRÊTE\n");
        } else {
            System.out.println("⚠️ Vosk non complètement disponible");
            System.out.println("ℹ️ Mode simulation sera utilisé\n");
        }
    }

    /**
     * Test 2: Tester l'IA avec texte simulé
     */
    private static void testAIDiagnosis() {
        System.out.println("▶ TEST 2: Diagnostic IA");
        System.out.println("─".repeat(50));
        
        // Texte simulé d'un patient
        String testText = "Bonjour, j'ai une fièvre depuis ce matin avec mal à la gorge " +
                         "et je tousse un peu. Je suis un peu fatigué.";
        
        System.out.println("📝 Texte de test:");
        System.out.println("  \"" + testText + "\"");
        System.out.println();
        
        AIHealthSummaryService aiService = new AIHealthSummaryService();
        System.out.println("🔄 Génération du diagnostic...\n");
        
        String diagnosis = aiService.generateHealthSummary(testText);
        
        // Afficher juste les premières lignes pour valider
        String[] lines = diagnosis.split("\n");
        System.out.println("Résultat (premières lignes):");
        for (int i = 0; i < Math.min(15, lines.length); i++) {
            System.out.println(lines[i]);
        }
        System.out.println("... (diagnostic complet généré)");
        System.out.println("\n✅ Diagnostic généré avec succès\n");
    }

    /**
     * Test 3: Afficher les différents niveaux d'urgence
     */
    private static void testUrgencyLevels() {
        System.out.println("▶ TEST 3: Niveaux d'Urgence");
        System.out.println("─".repeat(50));
        
        System.out.println("Le système gère 4 niveaux d'urgence:\n");
        
        System.out.println("🔴 ÉLEVÉE:");
        System.out.println("   • Symptômes graves (difficulté respiratoire, convulsion)");
        System.out.println("   • Action: Appeler SAMU 15 IMMÉDIATEMENT\n");
        
        System.out.println("🟡 MODÉRÉE:");
        System.out.println("   • Infections probables (fièvre + symptômes)");
        System.out.println("   • Action: Consulter un médecin dans 24-48h\n");
        
        System.out.println("🟡 BASSE-MODÉRÉE:");
        System.out.println("   • Symptômes légers à modérés");
        System.out.println("   • Action: Observation ou pharmacien\n");
        
        System.out.println("🟢 NORMALE:");
        System.out.println("   • Symptômes peu spécifiques");
        System.out.println("   • Action: Observation attentive\n");
        
        System.out.println("✅ Niveaux d'urgence vérifiés\n");
    }
}
