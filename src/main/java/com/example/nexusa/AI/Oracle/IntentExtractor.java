package com.example.nexusa.AI.Oracle;

import com.example.nexusa.AI.Oracle.dto.QueryIntent;
import com.example.nexusa.Model.Central.CentralCivilization;
import com.example.nexusa.Repository.Central.CentralCivilizationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IntentExtractor {
    private List<CentralCivilization> allCivs;
    private final CentralCivilizationRepository civRepo;
    private static final double FUZZY_THRESHOLD = 0.75; // 0–1, higher = stricter
    private static final int    MAX_CANDIDATE_LEN = 40;  // ignore very long windows
    private static final Map<String, List<String>> TOPIC_KEYWORDS = Map.of(
            "governance",  List.of("governance", "administration", "ruler", "king", "empire", "law", "political"),
            "trade",       List.of("trade", "commerce", "merchant", "exchange", "goods", "market"),
            "military",    List.of("military", "war", "army", "battle", "conquest", "soldier", "weapon"),
            "technology",  List.of("technology", "tools", "construction", "engineering", "brick", "craft"),
            "culture",     List.of("culture", "religion", "ritual", "art", "script", "language")
    );

    private static final Pattern YEAR_PATTERN =
            Pattern.compile("(\\d{3,4})\\s*(BCE|CE|AD|BC)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RANGE_PATTERN =
            Pattern.compile("between\\s+(\\d{3,4})\\s*(BCE|CE)?\\s+and\\s+(\\d{3,4})\\s*(BCE|CE|AD|BC)",
                    Pattern.CASE_INSENSITIVE);

    // Words that are NEVER part of a civilization name — only skip single-token candidates
    private static final Set<String> QUERY_FILLERS = Set.of(
            "tell", "me", "about", "what", "was", "how", "did", "explain", "describe",
            "show", "give", "find", "list", "get", "search",
            "during", "like", "a", "an", "is", "are", "were", "has", "have",
            "sources", "for", "from", "in", "on", "at", "by", "with", "to", "do", "you", "know",
            "database", "records", "akshara", "aksharanexus", "nexus", "oracle",
            "history", "historical", "ancient", "civilization", "civilisation"
            // NOTE: do NOT add "valley", "indus", "roman", "greek" — those are civ name parts
    );
    @PostConstruct
    public void init() {
        allCivs = civRepo.findAll(); // load once
    }
    public QueryIntent extract(String query) {
        QueryIntent intent = new QueryIntent();
        intent.setRawQuery(query);
        String lower = query.toLowerCase();

        extractYears(query, intent);

        for (Map.Entry<String, List<String>> entry : TOPIC_KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(lower::contains)) {
                intent.setTopic(entry.getKey());
                break;
            }
        }

        intent.setKeywords(extractKeywords(lower));
        intent.setCivilizationName(resolveCivName(query));

        return intent;
    }

    // ── Civilization resolution ───────────────────────────────────────────────

    /**
     * Multi-strategy resolution:
     * 1. Sliding-window n-gram: tries ALL windows, picks longest candidate that hits DB
     *    (fixes early-exit bug where "nexus"/"akshara" could match before "indus valley")
     * 2. Capitalised proper-noun sequence from original query
     * 3. Individual content words as last resort
     */
    private String resolveCivName(String query) {
        String[] tokens = query.split("\\s+");

        // Strategy 1: sliding-window n-gram — longest DB hit wins
        String bestMatch = null;
        int bestCandidateLen = 0;

        for (int len = tokens.length; len >= 1; len--) {
            for (int start = 0; start <= tokens.length - len; start++) {
                String candidate = String.join(" ", Arrays.copyOfRange(tokens, start, start + len))
                        .replaceAll("[^a-zA-Z0-9 ]", "").trim();
                if (candidate.isBlank()) continue;
                if (len == 1 && QUERY_FILLERS.contains(candidate.toLowerCase())) continue;

                List<CentralCivilization> matches = allCivs.stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(candidate)
                                || c.getTitle().toLowerCase().contains(candidate.toLowerCase()))
                        .toList();                if (!matches.isEmpty() && candidate.length() > bestCandidateLen) {
                    bestCandidateLen = candidate.length();
                    bestMatch = matches.getFirst().getTitle();
                }
            }
        }
        if (bestMatch != null) return bestMatch;

        // Strategy 2: capitalised proper-noun sequences
        Pattern proper = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\b");
        Matcher m = proper.matcher(query);
        while (m.find()) {
            String candidate = m.group(1).trim();
            if (QUERY_FILLERS.contains(candidate.toLowerCase())) continue;
            List<CentralCivilization> matches = allCivs.stream()
                    .filter(c -> c.getTitle().equalsIgnoreCase(candidate)
                            || c.getTitle().toLowerCase().contains(candidate.toLowerCase()))
                    .toList();            if (!matches.isEmpty()) return matches.getFirst().getTitle();
        }

        // Strategy 3: individual content words
        for (String token : tokens) {
            String word = token.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (word.length() < 4 || QUERY_FILLERS.contains(word)) continue;
            List<CentralCivilization> matches = civRepo.findByTitleContainingIgnoreCase(word);
            if (!matches.isEmpty()) return matches.getFirst().getTitle();
        }

        // ── Strategy 4: fuzzy Levenshtein fallback ────────────────────────────
        // Only runs when all exact strategies miss — e.g. "Indes Valey" or "Mesopotamiaa"
        return fuzzyResolveCivName(tokens);
    }
    // ── Conversation-aware entry point ────────────────────────────────────────

    /**
     * Extracts intent from a query that has already been rewritten,
     * but also accepts the last resolved civilization from conversation
     * history as a fallback when the rewritten query still has no civ.
     */
    public QueryIntent extract(String query, String fallbackCivilization) {
        QueryIntent intent = extract(query); // existing logic unchanged
        if ((intent.getCivilizationName() == null || intent.getCivilizationName().isBlank())
                && fallbackCivilization != null && !fallbackCivilization.isBlank()) {
            intent.setCivilizationName(fallbackCivilization);
        }
        return intent;
    }
    public String[] extractTwoCivilizations(String query) {
        // Run the full sliding-window resolution twice with different seeds
        // Simple approach: split on "vs", "versus", "and", "compare"
        String lower = query.toLowerCase();
        String[] parts = lower.split(" vs | versus | compare | and | between | difference ");
        if (parts.length >= 2) {
            String civ1 = resolveCivName(parts[0].trim());
            String civ2 = resolveCivName(parts[1].trim());
            if (civ1 != null && civ2 != null) return new String[]{civ1, civ2};
        }
        return new String[]{null, null};
    }

    /**
     * Compares every n-gram candidate window against ALL stored civ titles
     * using normalised Levenshtein similarity. Returns the best match above
     * FUZZY_THRESHOLD, or null if nothing is close enough.
     *
     * Runs only when exact strategies fail — the civRepo.findAll() call is
     * acceptable at that point (small reference table, already in L2 cache).
     */
    private String fuzzyResolveCivName(String[] tokens) {
        List<CentralCivilization> allCivs = civRepo.findAll();
        if (allCivs.isEmpty()) return null;

        String bestTitle = null;
        double bestScore = 0.0;

        for (int len = tokens.length; len >= 1; len--) {
            for (int start = 0; start <= tokens.length - len; start++) {
                String candidate = String.join(" ", Arrays.copyOfRange(tokens, start, start + len))
                        .replaceAll("[^a-zA-Z0-9 ]", "").trim().toLowerCase();

                if (candidate.isBlank()) continue;
                if (candidate.length() > MAX_CANDIDATE_LEN) continue;
                if (len == 1 && QUERY_FILLERS.contains(candidate)) continue;

                for (CentralCivilization civ : allCivs) {
                    double score = similarity(candidate, civ.getTitle().toLowerCase());
                    if (score > bestScore) {
                        bestScore = score;
                        bestTitle = civ.getTitle();
                    }
                }
            }
        }

        return bestScore >= FUZZY_THRESHOLD ? bestTitle : null;
    }

    /**
     * Normalised Levenshtein similarity in [0, 1].
     * similarity("indes valey", "indus valley") ≈ 0.85
     */
    private double similarity(String a, String b) {
        int dist = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) dist / maxLen;
    }

    /**
     * Standard iterative Levenshtein distance — O(m×n) time, O(n) space.
     * No external dependencies needed.
     */
    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1], curr = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }
    // ── Year extraction ───────────────────────────────────────────────────────

    private void extractYears(String query, QueryIntent intent) {
        Matcher rangeMatcher = RANGE_PATTERN.matcher(query);
        if (rangeMatcher.find()) {
            long y1 = Long.parseLong(rangeMatcher.group(1));
            long y2 = Long.parseLong(rangeMatcher.group(3));
            intent.setStartYear(isBCE(rangeMatcher.group(2)) ? -y1 : y1);
            intent.setEndYear(isBCE(rangeMatcher.group(4)) ? -y2 : y2);
            return;
        }

        Matcher m = YEAR_PATTERN.matcher(query);
        List<Long> years = new ArrayList<>();
        List<String> eras = new ArrayList<>();
        while (m.find()) {
            years.add(Long.parseLong(m.group(1)));
            eras.add(m.group(2));
        }

        if (years.size() == 1) {
            long y = isBCE(eras.getFirst()) ? -years.getFirst() : years.getFirst();
            intent.setStartYear(y - 100);
            intent.setEndYear(y + 100);
        } else if (years.size() >= 2) {
            intent.setStartYear(isBCE(eras.get(0)) ? -years.get(0) : years.get(0));
            intent.setEndYear(isBCE(eras.get(1)) ? -years.get(1) : years.get(1));
        }
    }

    private boolean isBCE(String era) {
        return era != null && (era.equalsIgnoreCase("BCE") || era.equalsIgnoreCase("BC"));
    }

    // ── Keyword extraction ────────────────────────────────────────────────────

    private List<String> extractKeywords(String lower) {
        Set<String> stopwords = Set.of(
                "what", "how", "tell", "me", "about", "was", "did",
                "the", "a", "an", "in", "of", "and", "or", "during", "between", "explain",
                "show", "sources", "for", "from", "give", "list", "find"
        );
        return Arrays.stream(lower.split("\\s+"))
                .filter(w -> w.length() > 3 && !stopwords.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }
}