package com.example.nexusa.AI.Oracle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String PRIMARY_MODEL  = "gpt-4o";
    private static final String FALLBACK_MODEL = "gpt-4o-mini";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String systemPrompt, String userMessage,
                           List<Map<String, String>> history) {
        String result = callLLM(PRIMARY_MODEL, systemPrompt, userMessage, history);
        if (result.startsWith("RATE_LIMITED:")) {
            System.err.println("Primary model rate-limited, falling back to " + FALLBACK_MODEL);
            result = callLLM(FALLBACK_MODEL, systemPrompt, userMessage, history);
        }
        return result;
    }

    public String generateFast(String systemPrompt, String userMessage,
                               List<Map<String, String>> history) {
        return callLLM(FALLBACK_MODEL, systemPrompt, userMessage, history);
    }

    private String callLLM(String model, String systemPrompt, String userMessage,
                           List<Map<String, String>> history) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Build message list: system → history → current user message
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (Map<String, String> turn : history) {
                messages.add(Map.of("role", turn.get("role"), "content", turn.get("content")));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 4096,
                    "messages", messages
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
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
                String errorMsg  = root.path("error").path("message").asText("Unknown API error");
                String errorType = root.path("error").path("type").asText("");
                return "LLM API error [%d] %s: %s".formatted(response.statusCode(), errorType, errorMsg);
            }

            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                return "Error: The AI model returned an empty response. Raw: "
                        + response.body().substring(0, Math.min(300, response.body().length()));
            }
            return content.asText();

        } catch (Exception e) {
            return "Error generating response: " + e.getMessage();
        }
    }
}