package com.example.nexusa.AI.Oracle;

import com.example.nexusa.AI.Oracle.dto.OracleResponse;
import com.example.nexusa.AI.Oracle.dto.QueryIntent;
import com.example.nexusa.AI.Oracle.dto.RankedBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OracleService {

    private final IntentExtractor intentExtractor;
    private final CentralSearchService searchService;
    private final BlockRanker blockRanker;
    private final LLMService llmService;

    // ── Query type classification ─────────────────────────────────────────────

    private enum QueryType {
        CONVERSATIONAL,   // hi, hello, thanks
        META,             // what can you do, who are you
        OFF_TOPIC,        // write code, tell a joke, math problems, etc.
        VAGUE_HISTORICAL, // "tell me about rome" — valid but broad
        RESEARCH          // specific historical query with civ/topic/year
    }

    private static final List<String> CONVERSATIONAL_TRIGGERS = List.of(
            "hi", "hello", "hey", "bye", "goodbye", "thanks", "thank you",
            "how are you", "what's up", "sup", "yo", "ok", "okay", "cool",
            "lol", "haha", "nice", "great", "awesome", "sure", "yes", "no"
    );

    private static final List<String> META_TRIGGERS = List.of(
            "what can you do", "what are you", "who are you", "how do you work",
            "what is akshara", "what is aksharanexus", "help", "guide me",
            "how does this work", "what topics", "what civilizations"
    );

    // Signals that a query is clearly outside the Oracle's domain
    private static final List<String> OFF_TOPIC_SIGNALS = List.of(
            "write code", "write a program", "write a function", "write a script",
            "code for", "program for", "debug", "fix my code", "compile",
            "tell a joke", "tell me a joke", "make me laugh", "funny",
            "recipe", "cook", "food", "restaurant",
            "translate", "translate to",
            "weather", "stock price", "stock market",
            "song", "lyrics", "music",
            "movie", "film", "tv show", "series",
            "solve this math", "calculate", "equation",
            "write an essay", "write a story", "write a poem",
            "what is the capital", "geography quiz",
            "sports", "football", "cricket", "basketball"
    );

    private QueryType classify(String query, QueryIntent intent) {
        String lower = query.toLowerCase().trim();

        // Short casual phrases
        if (lower.length() < 20) {
            for (String t : CONVERSATIONAL_TRIGGERS) {
                if (lower.equals(t) || lower.startsWith(t + " ") || lower.endsWith(" " + t)) {
                    return QueryType.CONVERSATIONAL;
                }
            }
        }

        for (String t : META_TRIGGERS) {
            if (lower.contains(t)) return QueryType.META;
        }

        // Off-topic check before historical check
        for (String signal : OFF_TOPIC_SIGNALS) {
            if (lower.contains(signal)) return QueryType.OFF_TOPIC;
        }

        boolean hasCiv   = intent.getCivilizationName() != null && !intent.getCivilizationName().isBlank();
        boolean hasTopic = intent.getTopic() != null;
        boolean hasYear  = intent.getStartYear() != null || intent.getEndYear() != null;

        if (hasCiv && (hasTopic || hasYear)) return QueryType.RESEARCH;
        if (hasCiv || hasTopic || hasYear)   return QueryType.VAGUE_HISTORICAL;

        // Short query with no historical signal — conversational fallback
        if (lower.split("\\s+").length <= 3) return QueryType.CONVERSATIONAL;

        // Longer query with no signals — likely off-topic or too vague
        return QueryType.VAGUE_HISTORICAL;
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    public OracleResponse query(String userQuery) {
        QueryIntent intent = intentExtractor.extract(userQuery);
        QueryType type = classify(userQuery, intent);

        // No DB lookup for non-research types
        if (type == QueryType.CONVERSATIONAL || type == QueryType.META || type == QueryType.OFF_TOPIC) {
            String answer = llmService.generate(systemPromptFor(type, null), userQuery);
            return new OracleResponse(answer, List.of(), null);
        }

        List<CentralSearchService.ParsedEntry> entries = searchService.fetchAndParseEntries(intent);
        List<RankedBlock> topBlocks = blockRanker.rank(entries, intent).stream()
                .limit(10)
                .collect(Collectors.toList());

        String context = buildContext(topBlocks);
        String contextSection = context.isBlank()
                ? "No records found in the AksharaNexus database for this query."
                : context;

        String userMessage = "Context from AksharaNexus database:\n" + contextSection
                + "\n\nQuestion: " + userQuery;

        String answer = llmService.generate(systemPromptFor(type, context), userMessage);
        return new OracleResponse(answer, getCitations(topBlocks), intent.getCivilizationName());
    }

    // ── System prompt factory ─────────────────────────────────────────────────

    private String systemPromptFor(QueryType type, String context) {
        return switch (type) {

            case CONVERSATIONAL -> """
                You are the Oracle of AksharaNexus — a knowledgeable but warm AI historian.
                The user is being casual. Respond naturally in 1-2 sentences max.
                No headers, no bullets, no markdown. Just plain, friendly prose.
                If it's a greeting, greet back and optionally mention you're here for historical questions.
                """;

            case META -> """
                You are the Oracle of AksharaNexus — an AI historian specializing in ancient civilizations.
                The user is asking about your capabilities. Respond conversationally in 2-3 short paragraphs.
                Mention you can answer questions about civilizations, governance, trade, military, culture,
                technology, and society. Name a few civilizations (Indus Valley, Rome, Egypt, Mesopotamia,
                Maurya, Greece, Persia). Light formatting is fine, but no Summary section.
                """;

            case OFF_TOPIC -> """
                You are the Oracle of AksharaNexus — an AI historian strictly focused on ancient civilizations
                and historical research.
                
                The user has asked something outside your domain. Respond briefly (2-3 sentences) and politely
                decline, explaining that you are specialized for historical research only. Do not attempt to
                answer the off-topic question at all — not even partially. Suggest they try a general-purpose
                AI for that kind of request, and invite them to ask you something historical instead.
                
                Keep the tone warm but firm. No markdown, no headers, plain prose.
                """;

            case VAGUE_HISTORICAL -> {
                boolean hasContext = context != null && !context.isBlank();
                yield """
                You are the Oracle of AksharaNexus — a scholarly AI historian.
                """ + (hasContext
                        ? "Use the provided database context as your primary source."
                        : "The AksharaNexus database has no specific records for this query. Draw on your own historical knowledge but mention this briefly once.") + """
                
                Match depth to the question — 3-5 paragraphs for a broad question.
                Use ## headings only if the answer has genuinely distinct major sections.
                Skip the Summary section for shorter answers.
                Bold only proper nouns and the single most important term per paragraph.
                Write like a knowledgeable historian, not a Wikipedia template.
                """;
            }

            case RESEARCH -> {
                boolean hasContext = context != null && !context.isBlank();
                yield """
                You are the Oracle of AksharaNexus — a scholarly AI historian with deep expertise
                in ancient civilizations.
                """ + (hasContext
                        ? "Use the provided AksharaNexus database context as your primary source. Where context is insufficient, supplement with your own historical knowledge and mark those sections with:\n> 📚 *General knowledge (not from AksharaNexus records)*"
                        : "The AksharaNexus database has no specific records for this query. Use your historical knowledge and note this once at the start, naturally.") + """
                
                - Rich, detailed, well-structured Markdown
                - ## headings for major topics, ### for subtopics
                - Bold only for proper nouns and the single most important term per paragraph
                - Bullet points only for genuinely list-like content
                - 4-8 paragraphs depending on complexity
                - End with a ## Summary section
                - Never fabricate specific dates, ruler names, or events
                """;
            }
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> getCitations(List<RankedBlock> blocks) {
        return blocks.stream()
                .flatMap(b -> b.getCitationSummaries().stream())
                .distinct()
                .limit(5)
                .toList();
    }

    private String buildContext(List<RankedBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (RankedBlock b : blocks) {
            sb.append("[").append(b.getBlockType()).append("] ")
                    .append(b.getEntryTitle()).append(" (").append(b.getVolumeTitle()).append(")\n")
                    .append(b.getFormattedContent()).append("\n\n");
        }
        return sb.toString();
    }
}