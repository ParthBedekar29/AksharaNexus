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

    private static final String PRIMARY_MODEL  = "llama-3.3-70b-versatile";
    private static final String FALLBACK_MODEL = "llama-3.1-8b-instant";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String systemPrompt, String userMessage) {
        String result = callGroq(PRIMARY_MODEL, systemPrompt, userMessage);
        if (result.startsWith("RATE_LIMITED:")) {
            System.err.println("Primary model rate-limited, falling back to " + FALLBACK_MODEL);
            result = callGroq(FALLBACK_MODEL, systemPrompt, userMessage);
        }
        return result;
    }

    private String callGroq(String model, String systemPrompt, String userMessage) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 4096,
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

            if (response.statusCode() == 429) {
                return "RATE_LIMITED:" + root.path("error").path("message").asText("rate limit reached");
            }

            if (response.statusCode() != 200) {
                // other errors
            }

            if (response.statusCode() != 200) {
                String errorMsg  = root.path("error").path("message").asText("Unknown API error");
                String errorType = root.path("error").path("type").asText("");
                return "LLM API error [%d] %s: %s".formatted(response.statusCode(), errorType, errorMsg);
            }

            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                System.err.println("Groq empty response body: " + response.body());
                return "Error: The AI model returned an empty response. Raw: "
                        + response.body().substring(0, Math.min(300, response.body().length()));
            }

            return content.asText();

        } catch (Exception e) {
            return "Error generating response: " + e.getMessage();
        }
    }
}