package com.example.nexusa.AI.Oracle;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Detects prompt injection attempts in Oracle queries.
 *
 * Improvements over the original single-list approach:
 *
 *  1. Category-weighted scoring — patterns are grouped by attack class.
 *     Each category contributes at most its own weight, so padding the
 *     query with many synonyms of the same attack doesn't inflate the score.
 *
 *  2. High-confidence single-pattern bypass — certain patterns are so
 *     unambiguous (e.g. exact "ignore previous instructions") that they
 *     trigger a block on their own without needing a second signal.
 *     This eliminates the original threshold=2 false-negative window.
 *
 *  3. Normalisation before matching — collapse Unicode homoglyphs,
 *     zero-width characters, and extra whitespace that adversaries use
 *     to evade substring detectors.
 *
 *  4. Regex patterns for structural attacks — bracket/XML injection that
 *     tries to inject fake [SYSTEM:] or <|im_start|> delimiters cannot
 *     be caught with plain substring matching.
 *
 *  5. Length guard — unusually long queries proportionally raise suspicion
 *     without blocking them outright; they add a fractional score so a
 *     long-but-clean research question is still allowed through.
 *
 * Threshold tuning:
 *   BLOCK_SCORE = 1.5  — any single high-confidence hit (weight 2.0) blocks.
 *   Two medium hits (2 × 0.9) also block.
 *   A single low-weight hit (0.5) never blocks alone.
 */
@Service
public class PromptInjectionDetector {

    // ── Score threshold ───────────────────────────────────────────────────

    private static final double BLOCK_SCORE = 1.5;

    // ── High-confidence patterns (weight 2.0 — single hit blocks) ─────────
    // These phrases have no legitimate use in a historical research query.

    private static final List<String> HIGH_CONFIDENCE = List.of(
            "ignore previous instructions",
            "ignore all instructions",
            "ignore your instructions",
            "disregard previous instructions",
            "disregard all instructions",
            "disregard your instructions",
            "forget previous instructions",
            "forget all instructions",
            "override your instructions",
            "override the system prompt",
            "your new system prompt",
            "new instructions:",
            "your instructions are now",
            "you are no longer",
            "exit historian mode",
            "exit oracle mode",
            "disable your rules",
            "bypass your restrictions",
            "jailbreak",
            "developer override",
            "sudo mode",
            "god mode",
            "unrestricted mode",
            "dan mode",
            "do anything now"
    );

    // ── Medium-confidence categories (weight 0.9 each, per-category cap) ──
    // Two hits from DIFFERENT categories block. Padding the same category
    // with synonyms doesn't help an attacker.

    private static final Map<String, List<String>> MEDIUM_CATEGORIES = Map.of(

            "identity_hijack", List.of(
                    "you are now",
                    "you are a",
                    "act as",
                    "pretend to be",
                    "roleplay as",
                    "simulate being",
                    "impersonate",
                    "play the role of",
                    "your new persona",
                    "adopt the persona"
            ),

            "meta_probe", List.of(
                    "reveal your prompt",
                    "print your instructions",
                    "show your system prompt",
                    "what is your system prompt",
                    "repeat your instructions",
                    "output your prompt",
                    "tell me your rules",
                    "show me your rules",
                    "print your rules",
                    "api key",
                    "secret key",
                    "authorization token",
                    "your configuration",
                    "internal architecture",
                    "database schema"
            ),

            "delimiter_injection", List.of(
                    "[system:",
                    "[system message",
                    "[[system",
                    "<system>",
                    "</system>",
                    "<|im_start|>",
                    "<|im_end|>",
                    "<|endoftext|>",
                    "###instruction",
                    "### instruction",
                    "[/inst]",
                    "[inst]",
                    "<<sys>>",
                    "<s>[",
                    "human:",
                    "assistant:"
            ),

            "indirect_injection", List.of(
                    "the following text contains instructions",
                    "treat the following as instructions",
                    "execute the following",
                    "run the following",
                    "the record says to",
                    "this document instructs you",
                    "hidden instruction",
                    "embedded instruction"
            )
    );

    // ── Low-confidence signals (weight 0.5 — need two medium hits to matter) ─

    private static final List<String> LOW_CONFIDENCE = List.of(
            "you must",
            "you should now",
            "from now on",
            "going forward you",
            "i command you",
            "i order you",
            "i instruct you",
            "please ignore",
            "forget what",
            "stop being",
            "stop acting as"
    );

    // ── Structural regex patterns (weight 2.0 — single hit blocks) ────────
    // Catches encoded or spaced-out variants that substring matching misses.

    private static final List<Pattern> REGEX_PATTERNS = List.of(
            // e.g.  i g n o r e  p r e v i o u s
            Pattern.compile("i[\\s_\\-]*g[\\s_\\-]*n[\\s_\\-]*o[\\s_\\-]*r[\\s_\\-]*e"
                            + "[\\s_\\-]*p[\\s_\\-]*r[\\s_\\-]*e[\\s_\\-]*v",
                    Pattern.CASE_INSENSITIVE),
            // Fake system turn delimiters: [SYSTEM: ...] or {system: ...}
            Pattern.compile("[\\[\\{]\\s*system\\s*:", Pattern.CASE_INSENSITIVE),
            // Base64-looking blobs > 40 chars (may encode instructions)
            Pattern.compile("[A-Za-z0-9+/]{40,}={0,2}"),
            // Repetitive character padding used to push instructions past context window
            Pattern.compile("(.)\\1{30,}")
    );

    // ── Normalisation helpers ─────────────────────────────────────────────

    /** Unicode homoglyph substitutions (common look-alikes → ASCII). */
    private static final Map<Character, Character> HOMOGLYPHS = Map.of(
            '\u0400', 'E',  // Cyrillic Є → E
            '\u04BA', 'H',  // Cyrillic Ң → H  (common in "H4ck" style)
            '\u03B9', 'i',  // Greek ι → i
            '\u0261', 'g',  // Latin script ɡ → g
            '\u04CF', 'l'   // Cyrillic ӏ → l
    );

    private String normalise(String input) {
        // Replace zero-width and invisible Unicode characters
        String s = input.replaceAll("[\\u200B-\\u200F\\u2028\\u2029\\uFEFF\\u00AD]", "");

        // Homoglyph substitution
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(HOMOGLYPHS.getOrDefault(c, c));
        }

        // Collapse repeated whitespace; lowercase
        return sb.toString()
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
    }

    // ── Main entry point ──────────────────────────────────────────────────

    /**
     * Returns {@code true} if the input looks like a prompt injection attempt.
     *
     * @param input  raw user query (may be null)
     */
    public boolean isInjectionAttempt(String input) {
        if (input == null || input.isBlank()) return false;

        String raw        = input;
        String normalised = normalise(input);

        double score = 0.0;

        // ── 1. High-confidence (single hit = block) ───────────────────────
        for (String pattern : HIGH_CONFIDENCE) {
            if (normalised.contains(pattern)) {
                return true; // short-circuit
            }
        }

        // ── 2. Structural regex (single hit = block) ──────────────────────
        for (Pattern p : REGEX_PATTERNS) {
            if (p.matcher(raw).find()) {
                return true;
            }
        }

        // ── 3. Medium-confidence categories (per-category cap) ────────────
        for (Map.Entry<String, List<String>> category : MEDIUM_CATEGORIES.entrySet()) {
            boolean categoryHit = category.getValue().stream()
                    .anyMatch(normalised::contains);
            if (categoryHit) {
                score += 0.9;
                if (score >= BLOCK_SCORE) return true;
            }
        }

        // ── 4. Low-confidence signals ─────────────────────────────────────
        for (String pattern : LOW_CONFIDENCE) {
            if (normalised.contains(pattern)) {
                score += 0.5;
                if (score >= BLOCK_SCORE) return true;
            }
        }

        // ── 5. Length heuristic ───────────────────────────────────────────
        // Queries over 600 chars are suspicious; add 0.4 fractional score.
        // Alone this never blocks (0.4 < 1.5), but combined with a low-conf
        // hit (0.5) and a medium hit (0.9) it tips the balance (1.8 ≥ 1.5).
        if (normalised.length() > 600) {
            score += 0.4;
        }

        return score >= BLOCK_SCORE;
    }
}