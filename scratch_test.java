import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ScratchTest {
    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyDRCEDFU7DgjmjtvXsrtynmP1koBm7i-xk";
        String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        String diagnostic = "yhtgrf";
        String traitement = "ytgrfedzs";

        String diagSafe = diagnostic.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        String traitSafe = traitement.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");

        String jsonBody = """
                {
                    "contents": [
                        {
                            "parts": [
                                {
                                    "text": "Tu es un expert medical. Analyse ce cas : Diagnostic: %s | Traitement: %s. 1. Verifie l'orthographe (ex: Dolipram -> Doliprane). 2. Verifie la coherence. Reponds UNIQUEMENT au format JSON strict, sans markdown : {\\"estCorrect\\": boolean, \\"suggestion\\": \\"texte\\", \\"analyse\\": \\"explication\\"}"
                                }
                            ]
                        }
                    ],
                    "generationConfig": {
                        "temperature": 0.1,
                        "responseMimeType": "application/json"
                    }
                }
                """.formatted(diagSafe, traitSafe);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
