package com.example.nexusa.AI.Oracle;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStore {

    private static final int MAX_TURNS     = 8;   // 8 user+assistant pairs = 16 messages
    private static final long TTL_MS       = 30 * 60 * 1000L; // 30 min idle eviction

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────

    public void addUserTurn(String sessionId, String content) {
        getOrCreate(sessionId).add("user", content);
    }

    public void addAssistantTurn(String sessionId, String content) {
        getOrCreate(sessionId).add("assistant", content);
    }

    /** Returns an immutable snapshot for injection into the LLM call. */
    public List<Map<String, String>> getHistory(String sessionId) {
        Session s = sessions.get(sessionId);
        return s == null ? List.of() : s.snapshot();
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private Session getOrCreate(String sessionId) {
        evictStale();
        return sessions.computeIfAbsent(sessionId, k -> new Session());
    }

    private void evictStale() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now - e.getValue().lastUsedMs > TTL_MS);
    }

    private static final class Session {
        // Each element: {"role": "user"/"assistant", "content": "..."}
        private final LinkedList<Map<String, String>> turns = new LinkedList<>();
        volatile long lastUsedMs = System.currentTimeMillis();

        synchronized void add(String role, String content) {
            turns.addLast(Map.of("role", role, "content", content));
            // Keep only the last MAX_TURNS*2 messages (user + assistant pairs)
            while (turns.size() > MAX_TURNS * 2) turns.removeFirst();
            lastUsedMs = System.currentTimeMillis();
        }

        synchronized List<Map<String, String>> snapshot() {
            lastUsedMs = System.currentTimeMillis();
            return List.copyOf(turns);
        }
    }
}