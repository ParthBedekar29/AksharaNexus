package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Oracle.OracleService;
import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oracle")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OracleController {

    private final OracleService oracleService;

    @PostMapping("/query")
    public ResponseEntity<OracleResponse> query(@RequestBody QueryRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        OracleResponse response = oracleService.query(request.getQuery());
        return ResponseEntity.ok(response);
    }

    // Simple inline DTO — no need for a separate file
    @lombok.Data
    public static class QueryRequest {
        private String query;
    }
}