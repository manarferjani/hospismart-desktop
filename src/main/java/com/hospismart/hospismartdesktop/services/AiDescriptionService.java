package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.utils.ApiConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI Description Generator — mirrors AiDescriptionService.php
 * Uses Gemini API with smart local fallback.
 */
public class AiDescriptionService {

    private final HttpClient httpClient;
    private final Random random = new Random();

    public AiDescriptionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Generate a professional description for an event.
     * Tries Gemini API first, falls back to local template-based generation.
     */
    public String generateDescription(String titre, String type, String lieu,
                                       String dateDebut, String dateFin, String budget) {
        // Try Gemini API first
        if (ApiConfig.isConfigured(ApiConfig.GEMINI_API_KEY)) {
            String result = callGeminiApi(titre, type, lieu, dateDebut, dateFin, budget);
            if (result != null) return result;
        }

        // Fallback: local smart template-based generation
        return generateLocalDescription(titre, type, lieu, dateDebut, dateFin, budget);
    }

    private String callGeminiApi(String titre, String type, String lieu,
                                  String dateDebut, String dateFin, String budget) {
        try {
            String prompt = buildPrompt(titre, type, lieu, dateDebut, dateFin, budget);

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            JsonArray parts = new JsonArray();
            parts.add(textPart);

            JsonObject content = new JsonObject();
            content.add("parts", parts);

            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject genConfig = new JsonObject();
            genConfig.addProperty("temperature", 0.7);
            genConfig.addProperty("maxOutputTokens", 300);

            JsonObject body = new JsonObject();
            body.add("contents", contents);
            body.add("generationConfig", genConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + ApiConfig.GEMINI_API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Gemini API HTTP " + response.statusCode());
                return null;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

            if (root.has("error")) {
                System.err.println("Gemini API error: " + root.getAsJsonObject("error").get("message").getAsString());
                return null;
            }

            String text = root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Clean markdown headers
            text = text.replaceAll("^#+\\s*", "").trim();
            return text;

        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            return null;
        }
    }

    // ── Local AI (Template-based) ────────────────────────────────────────

    private String generateLocalDescription(String titre, String type, String lieu,
                                             String dateDebut, String dateFin, String budget) {
        String typeKey = type != null ? type.toLowerCase().trim() : "";
        List<String> parts = new ArrayList<>();

        // Opening sentence
        Map<String, String[]> ouvertures = new LinkedHashMap<>();
        ouvertures.put("réunion", new String[]{
                "Cette réunion professionnelle intitulée \"%s\" est organisée dans le cadre de la coordination des activités de notre établissement de santé.",
                "L'établissement organise la réunion \"%s\" afin de renforcer la collaboration entre les équipes médicales et administratives.",
                "Dans le cadre de l'amélioration continue de nos services de santé, la réunion \"%s\" rassemblera les acteurs clés de l'établissement."
        });
        ouvertures.put("reunion", ouvertures.get("réunion"));
        ouvertures.put("formation", new String[]{
                "La formation \"%s\" est organisée pour renforcer les compétences des professionnels de santé de notre établissement.",
                "Dans le cadre du développement professionnel continu, notre établissement propose la formation \"%s\".",
                "Cette session de formation intitulée \"%s\" s'inscrit dans la démarche qualité et le perfectionnement des pratiques médicales."
        });
        ouvertures.put("visite", new String[]{
                "La visite \"%s\" est programmée dans le cadre de l'évaluation et de l'amélioration de nos installations hospitalières.",
                "Notre établissement accueille la visite \"%s\" pour permettre un échange constructif sur nos pratiques et infrastructures."
        });
        ouvertures.put("maintenance", new String[]{
                "L'opération de maintenance \"%s\" est planifiée pour assurer le bon fonctionnement des équipements de notre établissement.",
                "Dans le cadre de la maintenance préventive, l'intervention \"%s\" est programmée pour garantir la fiabilité de nos installations."
        });

        String[] defaultOuvertures = {
                "L'événement \"%s\" est organisé par notre établissement de santé dans le cadre de ses activités institutionnelles.",
                "Notre établissement hospitalier a le plaisir d'organiser \"%s\", un événement dédié à l'amélioration de nos services."
        };

        String[] ouv = ouvertures.getOrDefault(typeKey, defaultOuvertures);
        parts.add(String.format(ouv[random.nextInt(ouv.length)], titre != null ? titre : "événement"));

        // Objective sentence
        Map<String, String[]> objectifs = new LinkedHashMap<>();
        objectifs.put("réunion", new String[]{
                "Cette rencontre vise à favoriser les échanges entre les différents services et à coordonner les actions prioritaires.",
                "L'objectif principal est de discuter des enjeux stratégiques et d'aligner les efforts des équipes sur les priorités de l'établissement."
        });
        objectifs.put("reunion", objectifs.get("réunion"));
        objectifs.put("formation", new String[]{
                "Les participants auront l'opportunité d'acquérir de nouvelles connaissances et de mettre à jour leurs pratiques professionnelles.",
                "Cette formation interactive combine apports théoriques et mises en situation pratiques pour un apprentissage optimal."
        });
        objectifs.put("visite", new String[]{
                "Cette visite permettra d'évaluer les installations, d'identifier les points d'amélioration et de partager les bonnes pratiques.",
                "L'objectif est de garantir la conformité de nos installations aux normes en vigueur et d'optimiser la qualité des soins."
        });
        objectifs.put("maintenance", new String[]{
                "Cette intervention vise à prévenir les pannes, optimiser les performances des équipements et garantir la sécurité des patients et du personnel.",
                "Les travaux porteront sur la vérification, le calibrage et la mise à jour des équipements concernés."
        });
        String[] defaultObjectifs = {
                "Cet événement s'inscrit dans la démarche d'excellence de notre établissement et vise à renforcer la qualité des services proposés.",
                "Il contribuera au rayonnement de notre établissement et au renforcement des liens entre les différents acteurs de la santé."
        };

        String[] obj = objectifs.getOrDefault(typeKey, defaultObjectifs);
        parts.add(obj[random.nextInt(obj.length)]);

        // Location & date details
        List<String> details = new ArrayList<>();
        if (lieu != null && !lieu.isBlank()) details.add("au " + lieu);
        if (dateDebut != null && !dateDebut.isBlank()) {
            String formatted = formatDateFrench(dateDebut);
            if (formatted != null) details.add("le " + formatted);
        }
        if (!details.isEmpty()) {
            parts.add("L'événement se tiendra " + String.join(", ", details) + ".");
        }

        // Budget mention
        if (budget != null && !budget.isBlank()) {
            try {
                double b = Double.parseDouble(budget.replace(",", "."));
                if (b > 0) {
                    parts.add(String.format("Un budget de %.2f TND a été alloué pour assurer le bon déroulement de cette initiative.", b));
                }
            } catch (NumberFormatException ignored) {}
        }

        // Closing
        String[] closings = {
                "La participation de l'ensemble des collaborateurs concernés est vivement encouragée.",
                "Tous les professionnels de l'établissement sont invités à y participer activement.",
                "Nous comptons sur la mobilisation de chacun pour faire de cet événement une réussite."
        };
        parts.add(closings[random.nextInt(closings.length)]);

        return String.join(" ", parts);
    }

    private String formatDateFrench(String dateStr) {
        try {
            String[] months = {"", "janvier", "février", "mars", "avril", "mai", "juin",
                    "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

            LocalDateTime dt;
            if (dateStr.contains("T") || dateStr.contains(" ")) {
                dt = LocalDateTime.parse(dateStr.replace(" ", "T"));
            } else {
                dt = LocalDateTime.parse(dateStr + "T00:00:00");
            }
            return dt.getDayOfMonth() + " " + months[dt.getMonthValue()] + " " + dt.getYear()
                    + " à " + dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(String titre, String type, String lieu,
                                String dateDebut, String dateFin, String budget) {
        StringBuilder sb = new StringBuilder();
        sb.append("Génère une description professionnelle et concise (3-5 phrases) en français pour un événement hospitalier :\n");
        if (titre != null && !titre.isBlank()) sb.append("- Titre : ").append(titre).append("\n");
        if (type != null && !type.isBlank()) sb.append("- Type : ").append(type).append("\n");
        if (lieu != null && !lieu.isBlank()) sb.append("- Lieu : ").append(lieu).append("\n");
        if (dateDebut != null && !dateDebut.isBlank()) sb.append("- Date de début : ").append(dateDebut).append("\n");
        if (dateFin != null && !dateFin.isBlank()) sb.append("- Date de fin : ").append(dateFin).append("\n");
        if (budget != null && !budget.isBlank()) sb.append("- Budget : ").append(budget).append(" TND\n");
        sb.append("\nPas de titre ni de markdown. Texte direct uniquement.");
        return sb.toString();
    }
}
