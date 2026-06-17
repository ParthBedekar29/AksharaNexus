package com.example.nexusa.AI.Oracle;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enforces per-user rate limits on /oracle/query.
 *
 * Identity resolution order:
 *  1. JWT principal (email) — authenticated users get their own bucket
 *  2. X-Forwarded-For header — for reverse-proxied IPs (Railway injects this)
 *  3. Remote address — direct connections / local dev
 *
 * On rejection: 429 Too Many Requests with a JSON error body and a
 * Retry-After header (seconds until ~1 token refills at current rate).
 */
@Component
public class OracleRateLimitFilter extends OncePerRequestFilter {
    private boolean isGreeting(String text) {
        if (text == null) return false;

        String q = text.trim().toLowerCase();

        return q.equals("hi")
                || q.equals("hello")
                || q.equals("hey")
                || q.equals("bye")
                || q.equals("goodbye")
                || q.equals("thanks")
                || q.equals("thank you")
                || q.equals("ok")
                || q.equals("okay");
    }
    private static final String ORACLE_PATH = "/oracle/query";

    /**
     * Seconds until approximately one token refills.
     * 1 / REFILL_RATE = 3600 / 300 = 12 seconds.
     */
    private static final int RETRY_AFTER_SECONDS = 12;

    private final RateLimiterService rateLimiter;

    public OracleRateLimitFilter(RateLimiterService rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to the Oracle query endpoint
        return !ORACLE_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // TEMP TESTING BYPASS FOR SIMPLE GREETINGS
        if ("POST".equalsIgnoreCase(request.getMethod())) {

            String body = request.getReader()
                    .lines()
                    .reduce("", String::concat);

            if (isGreeting(body)) {
                chain.doFilter(request, response);
                return;
            }
        }

        String identity = resolveIdentity(request);

        if (!rateLimiter.tryConsume(identity)) {
            long remaining = rateLimiter.remainingTokens(identity);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.getWriter().write(
                    """
                    {
                      "error": "rate_limited",
                      "message": "Too many queries. Please wait a moment before trying again.",
                      "retryAfterSeconds": %d
                    }
                    """.formatted(RETRY_AFTER_SECONDS)
            );
            return;
        }

        // Pass through and add remaining-quota header for frontend awareness
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(rateLimiter.remainingTokens(identity)));
        chain.doFilter(request, response);
    }

    // ── Identity resolution ───────────────────────────────────────────────

    private String resolveIdentity(HttpServletRequest request) {
        // 1. Authenticated principal (JWT email) — most accurate
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getPrincipal().toString();
        }

        // 2. Forwarded IP (Railway / nginx reverse proxy)
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated chain; first is the real client
            String clientIp = forwarded.split(",")[0].strip();
            if (!clientIp.isEmpty()) return "ip:" + clientIp;
        }

        // 3. Direct remote address
        return "ip:" + request.getRemoteAddr();
    }
}