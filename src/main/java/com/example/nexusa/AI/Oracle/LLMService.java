package com.example.nexusa.AI.Oracle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
@Service
public class LLMService {
    @Value("${groq.api.key}")
    private String apiKey;

    private static final String MODEL = "llama-3.3-70b-versatile";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String systemPrompt, String userMessage) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "max_tokens", 4096,  // ✅ was 1024
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            // ✅ Surface API-level errors (wrong model, rate limit, bad key, etc.)
            if (response.statusCode() != 200) {
                String errorMsg = root.path("error").path("message").asText("Unknown API error");
                String errorType = root.path("error").path("type").asText("");
                return "LLM API error [%d] %s: %s".formatted(response.statusCode(), errorType, errorMsg);
            }

            JsonNode content = root.path("choices").path(0).path("message").path("content");

            if (content.isMissingNode() || content.asText().isBlank()) {
                // ✅ Log the full raw body so you can diagnose in dev
                System.err.println("Groq empty response body: " + response.body());
                return "Error: The AI model returned an empty response. Raw: " + response.body().substring(0, Math.min(300, response.body().length()));
            }

            return content.asText();

        } catch (Exception e) {
            return "Error generating response: " + e.getMessage();
        }
    }
}