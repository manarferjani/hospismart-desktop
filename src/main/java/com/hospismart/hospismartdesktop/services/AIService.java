package com.hospismart.hospismartdesktop.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService {
    private final String apiKey;
    private final HttpClient client;

    // URL de base de l'API Gemini
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public AIService(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Calcule la priorite d'un motif de consultation en utilisant l'API Gemini.
     * Echelle : 5 (CRITIQUE), 4 (URGENT), 3 (MOYEN), 2 (STANDARD), 1 (NON-URGENT).
     * En cas d'echec de l'API, une methode locale par mots-cles est utilisee en
     * fallback.
     */
    public int calculerPriorite(String motif) {
        if (motif == null || motif.trim().isEmpty()) {
            return 2;
        }

        // Si la cle API n'est pas configuree
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("REMOVED")) {
            System.out.println("[AIService] Pas de cle API -> fallback local pour: " + motif);
            return calculerPrioriteLocale(motif);
        }

        try {
            // Echapper les caracteres speciaux pour le JSON
            String motifEscaped = motif.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            // Construction du corps JSON au format Gemini API
            String jsonBody = """
                    {
                        "contents": [
                            {
                                "parts": [
                                    {
                                        "text": "Tu es un algorithme de triage medical de haute precision. Ton unique but est de classer le motif de consultation selon l echelle de priorite suivante :\\n5 (CRITIQUE) : Danger vital immediat (ex: bebe inconscient, arret respiratoire, hemorragie massive, detresse respiratoire, levres bleues).\\n4 (URGENT) : Cas grave nécessitant une attention rapide (ex: fievre grave/elevee, douleur intense, enfant avec mal a respirer, blessure grave).\\n3 (MOYEN) : Symptomes moderes (ex: fievre, migraine, mal de dos normal, vomissement).\\n2 (STANDARD) : Symptomes legers et supportables (ex: mal de dos leger, rhume mineur, douleur faible, petit inconvenient).\\n1 (NON-URGENT) : Administratif, suivi, routine (ex: certificat, renouvellement, bilan).\\n\\nReponds UNIQUEMENT par le chiffre correspondant (1, 2, 3, 4 ou 5), sans aucun texte supplementaire.\\n\\nAnalyse ce motif medical : %s"
                                    }
                                ]
                            }
                        ],
                        "generationConfig": {
                            "temperature": 0,
                            "maxOutputTokens": 10
                        }
                    }
                    """
                    .formatted(motifEscaped);

            // Gemini utilise la cle API comme parametre de requete
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                int result = extractScore(response.body());
                System.out.println("[AIService] API Gemini -> priorite " + result + " pour: " + motif);
                return result;
            } else {
                System.err.println("Erreur API Gemini (HTTP " + response.statusCode() + "): " + response.body());
                return calculerPrioriteLocale(motif);
            }

        } catch (Exception e) {
            System.err.println("Erreur API AI: " + e.getMessage());
            return calculerPrioriteLocale(motif);
        }
    }

    /**
     * Analyse une prescription medicale (diagnostic + traitement) avec Gemini.
     * Retourne une suggestion au format JSON : { "estCorrect": bool, "suggestion": "texte", "analyse": "texte" }
     */
    public String analyserPrescription(String diagnostic, String traitement) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("REMOVED")) {
            return "{\"estCorrect\": true, \"suggestion\": \"" + traitement + "\", \"analyse\": \"Mode offline\"}";
        }

        try {
            String diagSafe = diagnostic
                    .replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
            String traitSafe = traitement
                    .replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");

            String jsonBody = """
            {
                "contents": [{
                    "parts": [{
                        "text": "Tu es un expert médical. Analyse ce cas:\\nDiagnostic: %s\\nTraitement: %s\\n\\nRègles STRICTES:\\n1. Corrige l'orthographe des médicaments (ex: dolipramm->Doliprane).\\n2. Vérifie si le traitement est médicalement COHÉRENT avec le diagnostic.\\n3. Réponds avec ce JSON strict sans markdown:\\n{\\"estCorrect\\": boolean, \\"coherent\\": boolean, \\"suggestion\\": \\"traitement avec orthographe corrigée\\", \\"analyse\\": \\"explication courte\\"}\\n- estCorrect=false si faute d'orthographe OU incohérence médicale\\n- coherent=false si le médicament ne correspond pas au diagnostic\\n- suggestion = traitement avec fautes corrigées seulement (ne change pas les médicaments)"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.1,
                    "maxOutputTokens": 2048,
                    "thinkingConfig": {
                        "thinkingBudget": 0
                    }
                }
            }
            """.formatted(diagSafe, traitSafe);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[AIService] HTTP " + response.statusCode());
            System.out.println("[AIService] Body: " + response.body());

            if (response.statusCode() == 200) {
                // Extraire le bloc "text" complet jusqu'à la fin du tableau parts
                Pattern pText = Pattern.compile("\"text\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*\\}\\s*\\]");
                Matcher mText = pText.matcher(response.body());
                if (mText.find()) {
                    String raw = mText.group(1)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                            .trim();

                    // Nettoyer les éventuels backticks markdown
                    raw = raw.replaceAll("^```json\\s*", "")
                            .replaceAll("^```\\s*", "")
                            .replaceAll("\\s*```$", "")
                            .trim();

                    System.out.println("[AIService] JSON extrait: " + raw);
                    return raw;
                }
            }

            return "{\"estCorrect\": true, \"suggestion\": \""
                    + traitSafe + "\", \"analyse\": \"Erreur API (Code " + response.statusCode() + ")\"}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"estCorrect\": true, \"suggestion\": \""
                    + traitement.replace("\"", "\\\"") + "\", \"analyse\": \"Erreur technique: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Extrait le score de priorite de la reponse JSON Gemini.
     */
    private int extractScore(String jsonResponse) {
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"\\s*(\\d)\\s*\"");
        Matcher matcher = pattern.matcher(jsonResponse);

        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }

        if (lastMatch != null) {
            int score = Integer.parseInt(lastMatch);
            if (score >= 1 && score <= 5) {
                return score;
            }
        }

        // Fallback : chercher un chiffre isole dans la reponse
        Pattern fallbackPattern = Pattern.compile("\"text\"\\s*:\\s*\"[^\"]*?(\\d)[^\"]*?\"");
        Matcher fallbackMatcher = fallbackPattern.matcher(jsonResponse);
        if (fallbackMatcher.find()) {
            String digit = null;
            do {
                digit = fallbackMatcher.group(1);
            } while (fallbackMatcher.find());

            if (digit != null) {
                int score = Integer.parseInt(digit);
                if (score >= 1 && score <= 5) {
                    return score;
                }
            }
        }

        return 2;
    }

    /**
     * Supprime les accents d'un texte pour une comparaison robuste.
     * Ex: "levres bleutees" -> "levres bleutees"
     */
    private static String stripAccents(String text) {
        if (text == null)
            return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    /**
     * Calcul de priorite local par analyse de symptomes (fallback sans API).
     * Utilise la normalisation des accents pour eviter les problemes d'encodage.
     */
    private int calculerPrioriteLocale(String motif) {
        // On travaille UNIQUEMENT avec le texte sans accents pour eviter tout probleme
        // d'encodage
        String m = stripAccents(motif.toLowerCase());

        System.out.println("[AIService] Analyse locale - texte normalise: " + m);

        // ====== PHASE 1 : Detection directe par mots-cles critiques ======
        String[] critique = {
                "crise cardiaque", "infarctus", "arret cardiaque", "arret du coeur",
                "avc", "accident vasculaire",
                "inconscient", "perte de connaissance",
                "ne reagit plus", "ne repond plus",
                "coma", "convulsions",
                "ne respire plus", "arret respiratoire",
                "etouffe", "suffocation", "detresse respiratoire",
                "hemorragie massive", "saigne abondamment",
                "choc anaphylactique", "douleur thoracique", "accident grave",
                "empoisonnement", "overdose", "noyade", "electrocution",
                "tentative de suicide", "pendaison"
        };
        for (String k : critique) {
            if (m.contains(k)) {
                System.out.println("[AIService] CRITIQUE detecte via: '" + k + "'");
                return 5;
            }
        }

        // ====== PHASE 2 : Scoring par symptomes (detection combinee) ======
        int score = 0;

        // --- Symptomes respiratoires ---
        boolean problemeRespiratoire = false;
        String[] respSymptomes = {
                "respirer", "respiration", "respiratoire",
                "souffle", "essouffl",
                "sifflement", "siffler",
                "dyspnee", "apnee",
                "mal a respirer", "du mal a respirer",
                "difficulte a respirer", "difficultesa respirer",
                "gene respiratoire",
                "oppression", "poitrine se creuse",
                "tirage", "wheezing"
        };
        for (String k : respSymptomes) {
            if (m.contains(k)) {
                problemeRespiratoire = true;
                score += 3;
                System.out.println("[AIService] Symptome respiratoire: '" + k + "'");
                break;
            }
        }

        // --- Cyanose (levres/doigts bleus = signe de gravite extreme) ---
        boolean cyanose = false;
        String[] cyanoseSymptomes = {
                "cyanose", "cyanose",
                "bleute", "bleuatre",
                "levres bleue", "levres violette",
                "doigts bleu", "visage bleu", "peau bleue",
                "deviennent bleute", "deviennent bleu"
        };
        for (String k : cyanoseSymptomes) {
            if (m.contains(k)) {
                cyanose = true;
                score += 4;
                System.out.println("[AIService] Cyanose detectee via: '" + k + "'");
                break;
            }
        }

        // --- Perte de conscience / non-reactivite ---
        boolean inconscience = false;
        String[] conscienceSymptomes = {
                "inconscient", "ne reagit plus", "ne repond plus",
                "inanime", "perte de connaissance",
                "evanouissement", "evanoui", "syncope",
                "ne bouge plus", "yeux revulse"
        };
        for (String k : conscienceSymptomes) {
            if (m.contains(k)) {
                inconscience = true;
                score += 4;
                System.out.println("[AIService] Inconscience detectee via: '" + k + "'");
                break;
            }
        }

        // --- Douleur thoracique / cardiaque ---
        boolean douleurThoracique = false;
        String[] cardiaqueSymptomes = {
                "douleur thoracique", "douleur poitrine", "douleur a la poitrine",
                "mal au coeur", "palpitations", "tachycardie", "bradycardie",
                "pouls faible", "pouls irregulier", "serrement"
        };
        for (String k : cardiaqueSymptomes) {
            if (m.contains(k)) {
                douleurThoracique = true;
                score += 3;
                System.out.println("[AIService] Symptome cardiaque: '" + k + "'");
                break;
            }
        }

        // --- Saignement ---
        boolean saignement = false;
        String[] saignementSymptomes = {
                "hemorragie", "saigne", "saignement", "sang",
                "hemoptysie", "vomit du sang", "crache du sang", "selles sanglantes"
        };
        for (String k : saignementSymptomes) {
            if (m.contains(k)) {
                saignement = true;
                score += 2;
                System.out.println("[AIService] Saignement: '" + k + "'");
                break;
            }
        }

        // --- Population vulnerable (enfant, bebe, nourrisson, personne agee) ---
        boolean vulnerable = false;
        String[] vulnerableTermes = {
                "bebe", "nourrisson", "enfant", "nouveau-ne", "nouveau ne",
                "personne agee", "femme enceinte", "enceinte", "grossesse"
        };
        for (String k : vulnerableTermes) {
            if (m.contains(k)) {
                vulnerable = true;
                score += 1;
                System.out.println("[AIService] Population vulnerable: '" + k + "'");
                break;
            }
        }

        // --- Signes de gravite imminente ---
        String[] graviteImminente = {
                "enormement", "beaucoup de mal", "empire", "s'aggrave",
                "de pire en pire", "tres mal", "agonise", "mourir",
                "panique", "urgence", "vite", "immediatement"
        };
        for (String k : graviteImminente) {
            if (m.contains(k)) {
                score += 1;
                System.out.println("[AIService] Gravite imminente: '" + k + "'");
                break;
            }
        }

        System.out.println("[AIService] Score=" + score
                + " | resp=" + problemeRespiratoire
                + " | cyanose=" + cyanose
                + " | inconsc=" + inconscience
                + " | cardiaque=" + douleurThoracique
                + " | vuln=" + vulnerable);

        // ====== PHASE 3 : Combinaisons dangereuses ======

        if (problemeRespiratoire && cyanose)
            return 5;
        if (problemeRespiratoire && inconscience)
            return 5;
        if (cyanose)
            return 5;
        if (inconscience)
            return 5;
        if (douleurThoracique)
            return score >= 5 ? 5 : 4;

        if (score >= 6)
            return 5;
        if (score >= 4)
            return 4;
        if (score >= 2)
            return problemeRespiratoire ? 4 : 3;

        // ====== PHASE 4 : Mots-cles simples ======

        String[] urgent = {
                "fracture", "os casse", "douleur intense",
                "brulure grave", "douleur insupportable",
                "fievre elevee", "fievre grave", "fievre severe", "forte fievre",
                "40", "41", "double de volume", "gonflement important", "deforme", "deformee", "ne peut plus bouger",
                "immobilite",
                "infection grave", "douleur abdominale",
                "empoisonnement", "intoxication",
                "blessure grave", "chute grave", "traumatisme", "commotion"
        };
        for (String k : urgent) {
            if (m.contains(k))
                return 4;
        }

        boolean estGrave = m.contains("grave") || m.contains("severe")
                || m.contains("aigue") || m.contains("intense")
                || m.contains("critique") || m.contains("urgente")
                || m.contains("urgent") || m.contains("enormement")
                || m.contains("violemment")
                || m.contains("insupportable") || m.contains("extreme");

        // Modificateurs de faible gravite qui baissent la priorite
        boolean estLeger = m.contains("leger") || m.contains("petit")
                || m.contains("faible") || m.contains("modere")
                || m.contains("benin") || m.contains("supportable")
                || m.contains("un peu") || m.contains("legere");

        String[] moyen = {
                "fievre", "grippe", "vomissement", "diarrhee",
                "gastro", "plaie", "coupure", "suture", "migraine",
                "brulure", "entorse", "allergie", "eruption", "infection",
                "otite", "angine", "toux", "vertige", "nausee",
                "mal de tete", "cheville", "genou",
                "mal de dos", "lombalgie", "sciatique"
        };
        for (String k : moyen) {
            if (m.contains(k)) {
                if (estGrave)
                    return 4;
                if (estLeger)
                    return 2; // Leger -> STANDARD
                return 3;
            }
        }

        String[] nonUrgent = {
                "certificat", "ordonnance", "renouvellement", "administratif",
                "vaccin", "bilan", "controle", "suivi",
                "consultation de routine", "check-up", "resultats", "avis medical"
        };
        for (String k : nonUrgent) {
            if (m.contains(k))
                return 1;
        }

        if (estGrave)
            return 4;
        if (problemeRespiratoire)
            return 4;
        if (vulnerable && score > 0)
            return 3;

        System.out.println("[AIService] -> STANDARD (aucun match)");
        return 2;
    }
}
