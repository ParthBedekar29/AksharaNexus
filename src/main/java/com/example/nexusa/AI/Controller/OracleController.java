package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Oracle.OracleService;
import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oracle")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OracleController {

    private final OracleService oracleService;

    @PostMapping("/query")
    public OracleResponse query(@RequestBody QueryRequest request,
                                Authentication authentication) {
        String sessionId = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? "user:" + authentication.getPrincipal().toString()
                : "ip:unknown";

        return oracleService.query(request.getQuery(), sessionId);
    }

    @lombok.Data
    public static class QueryRequest {
        private String query;
    }
}