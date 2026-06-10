package com.example.nexusa.AI.Oracle;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter for Oracle queries.
 *
 * Each user identity (email or IP) gets an independent bucket:
 *   - Capacity  : MAX_TOKENS  (burst allowance)
 *   - Refill    : REFILL_RATE tokens per second (continuous)
 *
 * Defaults give a user 10 queries up-front, then ~1 per 12 seconds
 * sustained (300 per hour), with a burst of 10 at once.
 *
 * All state is in-memory. On a single-instance Railway deployment this
 * is fine; if you ever scale horizontally, swap the ConcurrentHashMap
 * for a Redis-backed solution (e.g. Redisson RRateLimiter).
 */
@Service
public class RateLimiterService {

    // ── Configuration ─────────────────────────────────────────────────────

    /** Maximum tokens a bucket can hold (burst size). */
    private static final double MAX_TOKENS = 10.0;

    /**
     * Tokens added per second.
     * 300 queries / 3600 seconds ≈ 0.0833 → ~1 query every 12 s sustained.
     */
    private static final double REFILL_RATE = 300.0 / 3600.0;

    /**
     * How long (seconds) to keep a bucket after last use before evicting it.
     * Prevents unbounded memory growth from one-time visitors.
     */
    private static final long IDLE_EVICT_SECONDS = 3600L;

    // ── State ─────────────────────────────────────────────────────────────

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Attempt to consume one token for the given identity.
     *
     * @param identity  email address (authenticated) or IP (anonymous)
     * @return {@code true} if the query is allowed, {@code false} if rate-limited
     */
    public boolean tryConsume(String identity) {
        Bucket bucket = buckets.computeIfAbsent(identity, k -> new Bucket());
        boolean allowed = bucket.tryConsume();
        evictIdleBuckets();
        return allowed;
    }

    /**
     * How many whole tokens remain for this identity (for diagnostic headers).
     */
    public long remainingTokens(String identity) {
        Bucket bucket = buckets.get(identity);
        if (bucket == null) return (long) MAX_TOKENS;
        return (long) bucket.currentTokens();
    }

    // ── Token bucket ──────────────────────────────────────────────────────

    private static final class Bucket {

        private double   tokens;
        private long     lastRefillNanos;
        private long     lastUsedNanos;

        Bucket() {
            this.tokens         = MAX_TOKENS;
            this.lastRefillNanos = System.nanoTime();
            this.lastUsedNanos   = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            lastUsedNanos = System.nanoTime();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized double currentTokens() {
            refill();
            return tokens;
        }

        synchronized long idleSeconds() {
            return (System.nanoTime() - lastUsedNanos) / 1_000_000_000L;
        }

        private void refill() {
            long now     = System.nanoTime();
            double elapsed = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(MAX_TOKENS, tokens + elapsed * REFILL_RATE);
            lastRefillNanos = now;
        }
    }

    // ── Idle eviction ─────────────────────────────────────────────────────

    /**
     * Lazily evict buckets not used for IDLE_EVICT_SECONDS.
     * Called on every request — cheap because it only iterates entries
     * marked as idle (most production traffic keeps buckets warm).
     */
    private void evictIdleBuckets() {
        buckets.entrySet().removeIf(e -> e.getValue().idleSeconds() > IDLE_EVICT_SECONDS);
    }
}