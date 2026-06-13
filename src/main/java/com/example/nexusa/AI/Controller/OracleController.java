package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Oracle.OracleService;
import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/oracle")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class OracleController {

    private final OracleService oracleService;

    // ── Existing non-streaming endpoint (keep for timeline, comparative) ──────
    @PostMapping("/query")
    public OracleResponse query(@Valid @RequestBody QueryRequest request,
                                Authentication authentication) {
        String sessionId = resolveSession(authentication);
        return oracleService.query(request.getQuery(), sessionId);
    }

    // ── New streaming endpoint ────────────────────────────────────────────────
    @PostMapping("/query/stream")
    public SseEmitter queryStream(@Valid @RequestBody QueryRequest request,
                                  Authentication authentication) {
        String sessionId = resolveSession(authentication);
        // Timeout: 3 minutes — enough for the longest research answer
        SseEmitter emitter = new SseEmitter(180_000L);
        // Run in a virtual thread so the request thread isn't blocked
        Thread.ofVirtual().start(() ->
                oracleService.queryStreaming(request.getQuery(), sessionId, emitter));
        return emitter;
    }

    private String resolveSession(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? "user:" + authentication.getPrincipal().toString()
                : "ip:unknown";
    }

    @lombok.Data
    public static class QueryRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 800)
        private String query;
    }
}