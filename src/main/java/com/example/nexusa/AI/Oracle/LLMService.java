package com.example.nexusa.AI.Oracle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    // ── Non-streaming (used for rewriter, timeline intro, fast paths) ─────────

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

    // ── Streaming (used for main research answers) ────────────────────────────

    /**
     * Streams OpenAI token chunks into the provided SseEmitter.
     * Sends events named "token" for each chunk, "done" when complete,
     * and "error" if something goes wrong.
     * Returns the full assembled answer so OracleService can save it.
     */
    public String generateStreaming(String systemPrompt, String userMessage,
                                    List<Map<String, String>> history,
                                    SseEmitter emitter) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (Map<String, String> turn : history) {
                messages.add(Map.of("role", turn.get("role"), "content", turn.get("content")));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = Map.of(
                    "model", PRIMARY_MODEL,
                    "max_tokens", 4096,
                    "stream", true,
                    "messages", messages
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<java.io.InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 429) {
                emitter.send(SseEmitter.event().name("error").data("RATE_LIMITED"));
                emitter.complete();
                return "RATE_LIMITED:";
            }

            if (response.statusCode() != 200) {
                emitter.send(SseEmitter.event().name("error").data("API_ERROR"));
                emitter.complete();
                return "Error: API returned " + response.statusCode();
            }

            StringBuilder fullAnswer = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (data.equals("[DONE]")) break;
                        try {
                            JsonNode root = objectMapper.readTree(data);
                            JsonNode delta = root.path("choices").path(0).path("delta");
                            String token = delta.path("content").asText("");
                            if (!token.isEmpty()) {
                                fullAnswer.append(token);
                                // Escape for SSE — newlines must be encoded
                                String encoded = token
                                        .replace("\\", "\\\\")
                                        .replace("\n", "\\n");
                                emitter.send(SseEmitter.event().name("token").data(encoded));
                            }
                        } catch (Exception ignored) {
                            // Malformed chunk — skip silently
                        }
                    }
                }
            }

            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
            return fullAnswer.toString();

        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {}
            return "Error generating response: " + e.getMessage();
        }
    }

    // ── Internal non-streaming call ───────────────────────────────────────────

    private String callLLM(String model, String systemPrompt, String userMessage,
                           List<Map<String, String>> history) {
        try {
            HttpClient client = HttpClient.newHttpClient();

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

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

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
                return "Error: The AI model returned an empty response.";
            }
            return content.asText();

        } catch (Exception e) {
            return "Error generating response: " + e.getMessage();
        }
    }
}