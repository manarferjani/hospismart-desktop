package com.hospismart.hospismartdesktop.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

/**
 * Service d'IA pour générer un bilan de santé basé sur la reconnaissance vocale
 * Supporte French language avec analyse des symptômes
 */
public class AIHealthSummaryService {
    // Configuration OpenAI (optionnel)
    private static final String AI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String AI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final OkHttpClient httpClient;
    private final Gson gson;

    public AIHealthSummaryService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    /**
     * Génère un bilan de maladie en utilisant l'IA
     * @param voiceText Texte extrait de la reconnaissance vocale
     * @return Bilan de santé généré par l'IA
     */
    public String generateHealthSummary(String voiceText) {
        try {
            System.out.println("[AI] ========================");
            System.out.println("[AI] Génération du bilan IA");
            System.out.println("[AI] Texte reçu: " + voiceText.substring(0, Math.min(100, voiceText.length())) + "...");
            System.out.println("[AI] ========================");
            
            // Essayer d'utiliser l'API OpenAI si la clé est disponible
            if (AI_API_KEY != null && !AI_API_KEY.isEmpty() && !AI_API_KEY.equals("your_api_key_here")) {
                try {
                    System.out.println("[AI] Utilisation de l'API OpenAI...");
                    String result = callOpenAIAPI(voiceText);
                    System.out.println("[AI] ✅ Bilan généré par OpenAI");
                    return result;
                } catch (Exception e) {
                    System.err.println("[AI] ⚠️ OpenAI indisponible: " + e.getMessage());
                    System.out.println("[AI] Utilisation du mode démo...");
                }
            } else {
                System.out.println("[AI] ℹ️ API OpenAI non configurée");
                System.out.println("[AI] Utilisation du mode démo avec analyse locale");
            }
            
            return generateDemoSummary(voiceText);
        } catch (Exception e) {
            System.err.println("[AI] ❌ Erreur génération bilan: " + e.getMessage());
            e.printStackTrace();
            return generateDemoSummary(voiceText);
        }
    }

    /**
     * Appel à l'API OpenAI pour générer un bilan médical
     */
    private String callOpenAIAPI(String voiceText) throws IOException {
        // Créer le prompt pour le système médical en français
        String prompt = "Tu es un assistant médical IA professionnel parlant français. " +
                "Basé sur la description vocale suivante du patient, génère un bilan de maladie professionnel et structuré.\n\n" +
                "Description du patient:\n" + voiceText + "\n\n" +
                "Génère un bilan en français incluant les sections suivantes:\n" +
                "1. SYMPTÔMES IDENTIFIÉS\n" +
                "2. ANALYSE PRÉLIMINAIRE\n" +
                "3. DIAGNOSTIC PROBABLE\n" +
                "4. RECOMMANDATIONS MÉDICALES\n" +
                "5. NIVEAU D'URGENCE (Normale/Modérée/Élevée)\n\n" +
                "Sois concis et professionnel. Rappelle que ce bilan n'est pas un diagnostic officiel.";
        
        // Créer le body de la requête
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-3.5-turbo");
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 600);
        
        var messagesArray = gson.toJsonTree(new Object[]{
            new ChatMessage("system", "Tu es un assistant médical IA professionnel. Réponds toujours en français."),
            new ChatMessage("user", prompt)
        });
        requestBody.add("messages", messagesArray);
        
        // Créer et envoyer la requête
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(AI_API_URL)
            .header("Authorization", "Bearer " + AI_API_KEY)
            .header("Content-Type", "application/json")
            .post(body)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("[AI] ❌ Erreur API OpenAI: " + response.code() + " " + response.message());
                if (response.body() != null) {
                    System.err.println("[AI] Réponse: " + response.body().string());
                }
                throw new IOException("API OpenAI erreur " + response.code());
            }
            
            String responseBody = response.body().string();
            return parseOpenAIResponse(responseBody);
        }
    }

    /**
     * Parse la réponse d'OpenAI
     */
    private String parseOpenAIResponse(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String content = jsonResponse
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();
            
            System.out.println("[AI] ✅ Réponse parsée avec succès");
            return content;
        } catch (Exception e) {
            System.err.println("[AI] ❌ Erreur parsing réponse: " + e.getMessage());
            throw new RuntimeException("Impossible de parser la réponse IA", e);
        }
    }

    /**
     * Génère un bilan de démonstration avec analyse basée sur mots-clés en français
     */
    private String generateDemoSummary(String voiceText) {
        StringBuilder summary = new StringBuilder();
        summary.append("╔════════════════════════════════════════════════════╗\n");
        summary.append("║      BILAN DE SANTÉ - ANALYSE BASÉE SUR IA        ║\n");
        summary.append("║         (Mode Démonstration - Analyse Locale)     ║\n");
        summary.append("╚════════════════════════════════════════════════════╝\n\n");
        
        String lowerText = voiceText.toLowerCase();
        
        summary.append("📋 DESCRIPTION DU PATIENT:\n");
        summary.append("─".repeat(50)).append("\n");
        summary.append(voiceText).append("\n\n");
        
        // Analyse des symptômes
        summary.append("🔍 SYMPTÔMES IDENTIFIÉS:\n");
        summary.append("─".repeat(50)).append("\n");
        
        java.util.List<String> symptoms = analyzeSymptoms(lowerText);
        if (symptoms.isEmpty()) {
            summary.append("• Pas de symptômes spécifiques identifiés\n");
        } else {
            for (String symptom : symptoms) {
                summary.append("• ").append(symptom).append("\n");
            }
        }
        summary.append("\n");
        
        // Analyse
        summary.append("🩺 ANALYSE PRÉLIMINAIRE:\n");
        summary.append("─".repeat(50)).append("\n");
        summary.append(generateAnalysis(lowerText)).append("\n\n");
        
        // Diagnostic probable
        summary.append("💊 DIAGNOSTIC PROBABLE:\n");
        summary.append("─".repeat(50)).append("\n");
        summary.append(generateDiagnosis(lowerText, symptoms)).append("\n\n");
        
        // Recommandations
        summary.append("✅ RECOMMANDATIONS:\n");
        summary.append("─".repeat(50)).append("\n");
        java.util.List<String> recommendations = generateRecommendations(lowerText, symptoms);
        for (String rec : recommendations) {
            summary.append("• ").append(rec).append("\n");
        }
        summary.append("\n");
        
        // Urgence
        String urgency = assessUrgency(lowerText, symptoms);
        summary.append("⚠️ NIVEAU D'URGENCE:\n");
        summary.append("─".repeat(50)).append("\n");
        summary.append(urgency).append("\n\n");
        
        summary.append("─".repeat(50)).append("\n");
        summary.append("⚕️ REMARQUE IMPORTANTE:\n");
        summary.append("Ce bilan est généré automatiquement à titre indicatif.\n");
        summary.append("Il ne remplace pas un diagnostic médical professionnel.\n");
        summary.append("Consultez un médecin pour une évaluation complète.\n");
        summary.append("═".repeat(50)).append("\n");
        
        return summary.toString();
    }
    
    /**
     * Analyse les symptômes mentionnés en français
     */
    private java.util.List<String> analyzeSymptoms(String text) {
        java.util.List<String> symptoms = new java.util.ArrayList<>();
        
        java.util.Map<String, String> symptomMap = new java.util.HashMap<>();
        symptomMap.put("mal de tête|migraine|céphalée|mal à la tête", "Maux de tête / Migraine");
        symptomMap.put("toux|tousse", "Toux");
        symptomMap.put("rhume|rhinite", "Rhume");
        symptomMap.put("grippe|grippal", "Symptômes grippaux");
        symptomMap.put("fièvre|fiévreux", "Fièvre");
        symptomMap.put("mal à la gorge|angine|pharyngite", "Mal de gorge");
        symptomMap.put("nausée|nausées|vomir|vomissement", "Nausées/Vomissements");
        symptomMap.put("douleur|douleurs|mal", "Douleur");
        symptomMap.put("fatigue|fatigué|épuisement", "Fatigue");
        symptomMap.put("vertige|vertiges|étourdissement", "Vertiges");
        symptomMap.put("dyspnée|essoufflement|respiration difficile", "Difficultés respiratoires");
        symptomMap.put("diarrhée|constipation", "Troubles digestifs");
        symptomMap.put("allergie|allergique", "Allergies");
        
        for (java.util.Map.Entry<String, String> entry : symptomMap.entrySet()) {
            if (text.matches(".*\\b(" + entry.getKey() + ")\\b.*")) {
                symptoms.add(entry.getValue());
            }
        }
        
        return symptoms;
    }
    
    /**
     * Génère une analyse basée sur le texte
     */
    private String generateAnalysis(String text) {
        StringBuilder analysis = new StringBuilder();
        
        // Analyse composée
        boolean hasFever = text.contains("fièvre");
        boolean hasCough = text.contains("toux");
        boolean hasThroat = text.contains("gorge");
        boolean hasHeadache = text.contains("mal de tête") || text.contains("migraine");
        boolean hasFatigue = text.contains("fatigue") || text.contains("fatigué");
        boolean hasNausea = text.contains("nausée") || text.contains("vomir");
        boolean hasDifficulty = text.contains("respiration difficile") || text.contains("essoufflement");
        
        if (hasFever && (hasCough || hasThroat)) {
            analysis.append("Infection respiratoire probable (virale ou bactérienne). ");
            if (hasDifficulty) {
                analysis.append("La présence de difficultés respiratoires aggrave le tableau clinique. ");
            }
        }
        
        if (hasHeadache && hasFatigue) {
            analysis.append("Migraine ou céphalée de tension associée à la fatigue. ");
            analysis.append("Peut indiquer une infection virale générale. ");
        }
        
        if (hasNausea && hasHeadache) {
            analysis.append("Association de symptômes gastrique et neurologique. ");
            analysis.append("Peut suggérer une infection virale systémique. ");
        }
        
        if (analysis.length() == 0) {
            analysis.append("Symptômes peu spécifiques. Surveillance recommandée. ");
        }
        
        analysis.append("Un suivi médical est conseillé pour un diagnostic précis.");
        return analysis.toString();
    }
    
    /**
     * Génère un diagnostic probable basé sur les symptômes
     */
    private String generateDiagnosis(String text, java.util.List<String> symptoms) {
        // Analyse combinée des symptômes pour déterminer le diagnostic
        boolean hasFever = text.contains("fièvre");
        boolean hasCough = text.contains("toux");
        boolean hasThroat = text.contains("gorge") || text.contains("angine");
        boolean hasHeadache = text.contains("mal de tête") || text.contains("migraine");
        boolean hasFatigue = text.contains("fatigue");
        boolean hasNausea = text.contains("nausée") || text.contains("vomir");
        boolean hasDiarrhea = text.contains("diarrhée");
        boolean hasVertigo = text.contains("vertige") || text.contains("étourdissement");
        boolean hasDifficulty = text.contains("respiration difficile") || text.contains("essoufflement");
        boolean hasAllergy = text.contains("allergie");
        
        // Diagnostic par combinaison de symptômes
        if (hasFever && hasCough && hasThroat) {
            if (hasDifficulty) {
                return "INFECTION RESPIRATOIRE GRAVE PROBABLE\n" +
                       "• Pneumonie virale ou bactérienne\n" +
                       "• Bronchite aiguë sévère\n" +
                       "Action: CONSULTATION MÉDICALE URGENTE";
            } else {
                return "INFECTION RESPIRATOIRE MODÉRÉE PROBABLE\n" +
                       "• Grippe (influenza)\n" +
                       "• Pharyngite virale\n" +
                       "• Bronchite aiguë\n" +
                       "Action: Consultation médicale recommandée dans 24-48h";
            }
        } else if (hasFever && hasCough) {
            return "INFECTION VIRALE PROBABLE\n" +
                   "• Grippe ou rhume sévère\n" +
                   "• Infection respiratoire virale\n" +
                   "Action: Repos et suivi médical";
        }
        
        if (hasNausea && hasDiarrhea && !hasFever) {
            return "GASTROENTÉRITE PROBABLE\n" +
                   "• Infection gastro-intestinale virale\n" +
                   "• Intoxication alimentaire possible\n" +
                   "Action: Hydratation et repos";
        }
        
        if (hasHeadache && hasFatigue && hasFever) {
            return "SYNDROME GRIPPAL PROBABLE\n" +
                   "• Infection virale générale\n" +
                   "• Grippe ou infection virale similaire\n" +
                   "Action: Repos, hydratation, suivi des symptômes";
        }
        
        if (hasHeadache && !hasFever) {
            return "CÉPHALÉE/MIGRAINE PROBABLE\n" +
                   "• Migraine de tension\n" +
                   "• Céphalée vasculaire\n" +
                   "Action: Repos, hydratation, consultation si persistant";
        }
        
        if (hasAllergy) {
            return "RÉACTION ALLERGIQUE PROBABLE\n" +
                   "• Allergie environnementale\n" +
                   "• Allergie saisonnière\n" +
                   "Action: Antihistaminiques, identifier et éviter l'allergène";
        }
        
        if (symptoms.isEmpty()) {
            return "DIAGNOSTIC INDÉTERMINÉ\n" +
                   "Symptômes non spécifiques identifiés\n" +
                   "Action: Consultation médicale pour évaluation complète";
        }
        
        return "SYNDROME VIRAL POSSIBLE\n" +
               "Combinaison de symptômes suggérant une infection virale\n" +
               "Action: Surveillance des symptômes et consultation si aggravation";
    }
    
    /**
     * Génère des recommandations
     */
    private java.util.List<String> generateRecommendations(String text, java.util.List<String> symptoms) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        boolean hasFever = text.contains("fièvre");
        boolean hasCough = text.contains("toux");
        boolean hasThroat = text.contains("gorge") || text.contains("angine");
        boolean hasHeadache = text.contains("mal de tête") || text.contains("migraine");
        boolean hasFatigue = text.contains("fatigue");
        boolean hasNausea = text.contains("nausée") || text.contains("vomir");
        boolean hasDiarrhea = text.contains("diarrhée");
        boolean hasDifficulty = text.contains("respiration difficile") || text.contains("essoufflement");
        
        recommendations.add("🏥 Consulter un médecin ou un pharmacien pour évaluation complète");
        
        if (hasFever) {
            recommendations.add("📊 Mesurer la température régulièrement (matin et soir)");
            recommendations.add("💊 Prendre du paracétamol (500-1000mg) ou ibuprofène si nécessaire");
            recommendations.add("🌡️ Si température > 39°C, consulter rapidement un médecin");
        }
        
        if (hasCough || hasDifficulty) {
            recommendations.add("💧 Rester hydraté (eau, thé chaud, miel)");
            recommendations.add("⚠️ Éviter les irritants respiratoires (fumée, pollution)");
            recommendations.add("🛏️ Surélever la tête pour faciliter la respiration la nuit");
        }
        
        if (hasThroat) {
            recommendations.add("🧂 Gargariser avec de l'eau tiède salée plusieurs fois par jour");
            recommendations.add("🍵 Consommer des liquides chauds (thé, bouillon)");
            recommendations.add("🚫 Éviter les aliments épicés ou irritants");
        }
        
        if (hasHeadache) {
            recommendations.add("🌙 Reposer dans un environnement calme et sombre");
            recommendations.add("💧 S'assurer une hydratation suffisante");
            recommendations.add("🧘 Essayer des techniques de relaxation ou massage du cou");
        }
        
        if (hasFatigue) {
            recommendations.add("🛏️ Repos suffisant (8+ heures de sommeil par nuit)");
            recommendations.add("⏰ Prendre des pauses régulières pendant la journée");
            recommendations.add("🚫 Éviter les activités fatigantes jusqu'à amélioration");
        }
        
        if (hasNausea || hasDiarrhea) {
            recommendations.add("🚫 Éviter les aliments gras ou trop épicés");
            recommendations.add("🍌 Manger léger (riz, pain, bouillon de poulet)");
            recommendations.add("💊 Prendre un anti-diarrhéique si diarrhée sévère");
        }
        
        if (hasDifficulty) {
            recommendations.add("🚨 APPELER LE 15 (SAMU) EN CAS DE DIFFICULTÉ RESPIRATOIRE SÉVÈRE");
            recommendations.add("⚠️ Consulter aux urgences (pompiers, hôpital) immédiatement");
        } else if (hasFever && (hasCough || hasThroat)) {
            recommendations.add("⏰ Consulter un médecin dans les 24-48 heures");
        }
        
        recommendations.add("☎️ Appeler SAMU (15) si symptômes graves ou persistants");
        
        return recommendations;
    }
    
    /**
     * Évalue le niveau d'urgence basé sur les symptômes
     */
    private String assessUrgency(String text, java.util.List<String> symptoms) {
        String lowerText = text.toLowerCase();
        
        // Compteur de symptômes graves
        int severityScore = 0;
        
        // Symptômes graves (críticos)
        if (lowerText.contains("difficulté respiration") || lowerText.contains("essoufflement") ||
            lowerText.contains("perte conscience") || lowerText.contains("confusion") ||
            lowerText.contains("saignement") || lowerText.contains("convulsion") ||
            lowerText.contains("douleur thoracique") || lowerText.contains("paralysie") ||
            lowerText.contains("accident") || lowerText.contains("trauma")) {
            severityScore += 100;
        }
        
        // Symptômes modérés-graves
        if (lowerText.contains("fièvre") && lowerText.contains("toux")) {
            severityScore += 30;
        }
        if (lowerText.contains("douleur intense") || lowerText.contains("douleur severe")) {
            severityScore += 25;
        }
        if (lowerText.contains("nausée") && lowerText.contains("vomissement")) {
            severityScore += 20;
        }
        
        // Symptômes modérés
        if (lowerText.contains("fièvre")) {
            severityScore += 15;
        }
        if (lowerText.contains("mal de tête") || lowerText.contains("migraine")) {
            severityScore += 10;
        }
        if (symptoms.size() >= 4) {
            severityScore += 10;
        }
        
        // Évaluation finale
        if (severityScore >= 100) {
            return "🔴 ÉLEVÉE - URGENCE MÉDICALE\n" +
                   "Symptômes graves détectés.\n" +
                   "ACTION IMMÉDIATE: Appelez le 15 (SAMU) ou allez aux urgences\n" +
                   "Ne pas attendre, consulter un médecin immédiatement";
        } else if (severityScore >= 40) {
            return "🟡 MODÉRÉE - CONSULTATION RECOMMANDÉE\n" +
                   "Symptômes d'une infection probable.\n" +
                   "ACTION: Consulter un médecin dans les 24-48 heures\n" +
                   "Surveillance étroite des symptômes recommandée";
        } else if (severityScore >= 15) {
            return "🟠 BASSE-MODÉRÉE - AVIS MÉDICAL CONSEILLÉ\n" +
                   "Symptômes légers à modérés identifiés.\n" +
                   "ACTION: Consulter un pharmacien ou médecin si persistance\n" +
                   "Repos et auto-observation recommandés";
        } else {
            return "🟢 NORMALE - OBSERVATION ATTENTIVE\n" +
                   "Symptômes peu spécifiques identifiés.\n" +
                   "ACTION: Observation et consultation si aggravation\n" +
                   "Appelez le 15 en cas de symptômes graves";
        }
    }

    /**
     * Classe interne pour les messages de chat
     */
    private static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
