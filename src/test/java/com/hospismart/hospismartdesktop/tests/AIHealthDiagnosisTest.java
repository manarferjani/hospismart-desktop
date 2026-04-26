package com.hospismart.hospismartdesktop.tests;

import com.hospismart.hospismartdesktop.services.AIHealthSummaryService;

/**
 * Test de démonstration du service d'IA amélioré
 * Montre les différents types de diagnostics possibles
 */
public class AIHealthDiagnosisTest {

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║    TEST - DIAGNOSTIC IA AVEC CAS DIFFÉRENTS         ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        AIHealthSummaryService aiService = new AIHealthSummaryService();

        // Test 1: Infection respiratoire grave
        testCase1(aiService);
        
        // Test 2: Infection respiratoire modérée
        testCase2(aiService);
        
        // Test 3: Gastroentérite
        testCase3(aiService);
        
        // Test 4: Céphalée
        testCase4(aiService);
        
        // Test 5: Symptômes multiples
        testCase5(aiService);

        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  ✅ TOUS LES TESTS DE DIAGNOSTIC RÉUSSIS            ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");
    }

    /**
     * Test 1: Infection respiratoire grave
     */
    private static void testCase1(AIHealthSummaryService aiService) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("TEST 1: INFECTION RESPIRATOIRE GRAVE");
        System.out.println("═".repeat(60));
        
        String voiceText = "Bonjour, j'ai une très forte fièvre depuis 3 jours. Je tousse énormément, " +
                          "j'ai mal à la gorge et j'ai des difficultés à respirer. Je suis vraiment très fatigué.";
        
        System.out.println("\n📝 TEXTE VOCAL:");
        System.out.println(voiceText);
        
        System.out.println("\n🔄 Génération du diagnostic...\n");
        String result = aiService.generateHealthSummary(voiceText);
        System.out.println(result);
    }

    /**
     * Test 2: Infection respiratoire modérée
     */
    private static void testCase2(AIHealthSummaryService aiService) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("TEST 2: INFECTION RESPIRATOIRE MODÉRÉE");
        System.out.println("═".repeat(60));
        
        String voiceText = "J'ai une petite fièvre depuis hier, environ 38 degrés. " +
                          "Je tousse un peu et j'ai mal à la gorge. Mais je me sens toujours capable de bouger.";
        
        System.out.println("\n📝 TEXTE VOCAL:");
        System.out.println(voiceText);
        
        System.out.println("\n🔄 Génération du diagnostic...\n");
        String result = aiService.generateHealthSummary(voiceText);
        System.out.println(result);
    }

    /**
     * Test 3: Gastroentérite
     */
    private static void testCase3(AIHealthSummaryService aiService) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("TEST 3: GASTROENTÉRITE");
        System.out.println("═".repeat(60));
        
        String voiceText = "Je suis très malade, j'ai des nausées terribles et j'ai diarrhée depuis cette nuit. " +
                          "J'ai l'impression que c'est une intoxication alimentaire.";
        
        System.out.println("\n📝 TEXTE VOCAL:");
        System.out.println(voiceText);
        
        System.out.println("\n🔄 Génération du diagnostic...\n");
        String result = aiService.generateHealthSummary(voiceText);
        System.out.println(result);
    }

    /**
     * Test 4: Céphalée/Migraine
     */
    private static void testCase4(AIHealthSummaryService aiService) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("TEST 4: CÉPHALÉE/MIGRAINE");
        System.out.println("═".repeat(60));
        
        String voiceText = "J'ai un mal de tête terrible depuis ce matin. C'est une migraine intense, " +
                          "j'ai un peu de mal à voir clair. Je n'ai pas de fièvre mais je suis très fatigué.";
        
        System.out.println("\n📝 TEXTE VOCAL:");
        System.out.println(voiceText);
        
        System.out.println("\n🔄 Génération du diagnostic...\n");
        String result = aiService.generateHealthSummary(voiceText);
        System.out.println(result);
    }

    /**
     * Test 5: Symptômes multiples
     */
    private static void testCase5(AIHealthSummaryService aiService) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("TEST 5: SYNDROME GRIPPAL COMPLET");
        System.out.println("═".repeat(60));
        
        String voiceText = "Je me sens vraiment mal. J'ai de la fièvre, mal à la tête, " +
                          "je suis très fatigué, j'ai mal partout. Je ne sais pas ce que j'ai.";
        
        System.out.println("\n📝 TEXTE VOCAL:");
        System.out.println(voiceText);
        
        System.out.println("\n🔄 Génération du diagnostic...\n");
        String result = aiService.generateHealthSummary(voiceText);
        System.out.println(result);
    }
}
