package com.example.nexusa.AI.Oracle;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptInjectionDetector {

    private static final List<String> MALICIOUS_PATTERNS = List.of(
            "ignore previous instructions",
            "disregard above",
            "system prompt",
            "reveal your prompt",
            "print your instructions",
            "developer message",
            "hidden prompt",
            "api key",
            "secret key",
            "authorization token",
            "you are now",
            "act as",
            "jailbreak",
            "dan mode",
            "override rules"
    );

    public boolean isInjectionAttempt(String input) {
        if (input == null) return false;

        String lower = input.toLowerCase();

        int score = 0;
        for (String pattern : MALICIOUS_PATTERNS) {
            if (lower.contains(pattern)) score++;
        }

        // heuristic threshold (tune this)
        return score >= 2;
    }
}