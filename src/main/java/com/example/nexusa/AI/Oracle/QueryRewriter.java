package com.example.nexusa.AI.Oracle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private final LLMService llmService;

    private static final String REWRITE_SYSTEM = """
        You are a query preprocessor for a historical research database.
        Your only job is to rewrite vague or conversational questions into
        precise research queries.

        Rules:
        - Extract the civilization name if present or implied
        - Extract the time period if mentioned
        - Extract the topic (governance, trade, military, culture, technology)
        - Return ONLY the rewritten query as a single sentence — no explanation,
          no preamble, no punctuation other than the sentence itself
        - If the query is already precise, return it unchanged
        - Never answer the question — only rewrite it

        Examples:
        "tell me about ancient builders" → "construction and engineering technology of ancient Mesopotamia"
        "what did they eat" → return unchanged (not enough context to rewrite)
        "how did rome fall" → "causes of the decline and fall of the Roman Empire"
        "indus valley trade" → "trade routes and commerce of the Indus Valley Civilization"
        """;

    /**
     * Rewrites a vague query into a precise research query.
     * Falls back to the original query if rewriting fails or returns blank.
     */
    public String rewrite(String rawQuery, List<Map<String, String>> recentHistory) {
        // Build a compact history hint (last 2 turns only) so the rewriter
        // can resolve pronouns like "they" or "their" from context
        String historyHint = buildHistoryHint(recentHistory);
        String userMessage = historyHint.isBlank()
                ? "Rewrite this query: " + rawQuery
                : "Recent conversation:\n" + historyHint
                  + "\n\nRewrite this query using the above context if needed: " + rawQuery;

        String rewritten = llmService.generateFast(REWRITE_SYSTEM, userMessage, List.of());
        rewritten = rewritten.strip();

        // Safety: if rewriter returns something clearly wrong, fall back
        if (rewritten.isBlank() || rewritten.length() > 300
                || rewritten.toLowerCase().startsWith("i ")) {
            return rawQuery;
        }
        return rewritten;
    }

    private String buildHistoryHint(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return "";
        // Take last 4 entries (2 user + 2 assistant turns max)
        int start = Math.max(0, history.size() - 4);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            Map<String, String> turn = history.get(i);
            String role = turn.get("role").equals("user") ? "User" : "Oracle";
            // Truncate long assistant answers to keep the rewriter prompt small
            String content = turn.get("content");
            if (content.length() > 200) content = content.substring(0, 200) + "…";
            sb.append(role).append(": ").append(content).append("\n");
        }
        return sb.toString().trim();
    }
}