package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Oracle.OracleService;
import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@Valid @RequestBody QueryRequest request,
                             Authentication authentication) {
        String sessionId = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? "user:" + authentication.getPrincipal().toString()
                : "ip:unknown";

        SseEmitter emitter = new SseEmitter(120_000L);

        Thread.ofVirtual().start(() -> {
            try {
                oracleService.queryStream(request.getQuery(), sessionId, token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @lombok.Data
    public static class QueryRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 800)
        private String query;
    }
}