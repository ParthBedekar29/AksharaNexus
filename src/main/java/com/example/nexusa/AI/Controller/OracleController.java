package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Oracle.OracleService;
import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/oracle")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OracleController {

    private final OracleService oracleService;

    @PostMapping("/query")
    public OracleResponse query(@Valid @RequestBody QueryRequest request,
                                Authentication authentication) {
        String sessionId = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? "user:" + authentication.getPrincipal().toString()
                : "ip:unknown";
        return oracleService.query(request.getQuery(), sessionId);
    }
    @PostMapping(value = "/stream", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> stream(
            @Valid @RequestBody QueryRequest request,
            Authentication authentication) {

        String sessionId = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? "user:" + authentication.getPrincipal().toString()
                : "ip:unknown";

        StreamingResponseBody body = outputStream -> {
            try {
                oracleService.queryStream(request.getQuery(), sessionId, token -> {
                    try {
                        // Each token as {"t":"<text>"}\n  — newlines encoded in the value
                        String json = "{\"t\":" + asJsonString(token) + "}\n";
                        outputStream.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                // Sentinel
                outputStream.write("{\"done\":true}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception e) {
                try {
                    outputStream.write(("{\"error\":\"" + e.getMessage() + "\"}\n")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {}
            }
        };

        return ResponseEntity.ok()
                .header("Content-Type", "application/x-ndjson")
                .header("X-Accel-Buffering", "no")   // disables Railway/nginx buffering
                .header("Cache-Control", "no-cache")
                .body(body);
    }

    // Proper JSON string escaping
    private String asJsonString(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    @lombok.Data
    public static class QueryRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 800)
        private String query;
    }
}