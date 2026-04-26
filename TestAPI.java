import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestAPI {
    public static void main(String[] args) throws Exception {
        String token = "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";
        String url = "https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5";
        String body = "{\"inputs\": \"test\"}";

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
            
        System.out.println("Sending to: " + url);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
