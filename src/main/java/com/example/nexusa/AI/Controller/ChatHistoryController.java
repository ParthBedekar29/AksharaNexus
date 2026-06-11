package com.example.nexusa.AI.Controller;

import com.example.nexusa.Model.ChatMessage;
import com.example.nexusa.Model.ChatSession;
import com.example.nexusa.Model.PublicUser;
import com.example.nexusa.Repository.ChatMessageRepository;
import com.example.nexusa.Repository.ChatSessionRepository;
import com.example.nexusa.Repository.PublicUserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatHistoryController {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PublicUserRepository publicUserRepository;

    // ── List sessions ─────────────────────────────────────────────────────────
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(Principal principal) {
        PublicUser user = getUser(principal);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        List<SessionDTO> sessions = sessionRepository
                .findByPublicUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(s -> new SessionDTO(s.getId(), s.getTitle(), s.getUpdatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessions);
    }

    // ── Create session ────────────────────────────────────────────────────────
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest req,
                                           Principal principal) {
        PublicUser user = getUser(principal);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        ChatSession session = new ChatSession();
        session.setPublicUser(user);
        // Title = first 40 chars of the first message
        String title = req.getFirstMessage().length() > 40
                ? req.getFirstMessage().substring(0, 40).trim() + "…"
                : req.getFirstMessage();
        session.setTitle(title);
        sessionRepository.save(session);

        return ResponseEntity.ok(new SessionDTO(session.getId(), session.getTitle(), session.getUpdatedAt()));
    }

    // ── Get messages for a session ────────────────────────────────────────────
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable UUID sessionId,
                                         Principal principal) {
        Optional<ChatSession> opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("Session not found");
        if (!opt.get().getPublicUser().getUserId().equals(getUser(principal).getUserId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<MessageDTO> messages = messageRepository
                .findBySessionOrderByCreatedAtAsc(opt.get())
                .stream()
                .map(m -> new MessageDTO(m.getRole(), m.getContent()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }

    // ── Save a message pair (user + oracle) ───────────────────────────────────
    @PostMapping("/sessions/{sessionId}/messages")
    @Transactional
    public ResponseEntity<?> saveMessages(@PathVariable UUID sessionId,
                                          @RequestBody SaveMessagesRequest req,
                                          Principal principal) {
        Optional<ChatSession> opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("Session not found");
        ChatSession session = opt.get();
        if (!session.getPublicUser().getUserId().equals(getUser(principal).getUserId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("USER");
        userMsg.setContent(req.getUserMessage());
        messageRepository.save(userMsg);

        ChatMessage oracleMsg = new ChatMessage();
        oracleMsg.setSession(session);
        oracleMsg.setRole("ORACLE");
        oracleMsg.setContent(req.getOracleResponse());
        messageRepository.save(oracleMsg);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return ResponseEntity.ok("Messages saved.");
    }

    // ── Delete session ────────────────────────────────────────────────────────
    @DeleteMapping("/sessions/{sessionId}")
    @Transactional
    public ResponseEntity<?> deleteSession(@PathVariable UUID sessionId,
                                           Principal principal) {
        Optional<ChatSession> opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("Session not found");
        if (!opt.get().getPublicUser().getUserId().equals(getUser(principal).getUserId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        messageRepository.deleteAllBySession(opt.get());
        sessionRepository.delete(opt.get());
        return ResponseEntity.ok("Session deleted.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private PublicUser getUser(Principal principal) {
        return publicUserRepository.findByEmail(principal.getName()).orElse(null);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data public static class CreateSessionRequest { private String firstMessage; }
    @Data public static class SaveMessagesRequest {
        private String userMessage;
        private String oracleResponse;
    }
    @Data public static class SessionDTO {
        private final UUID id;
        private final String title;
        private final LocalDateTime updatedAt;
    }
    @Data public static class MessageDTO {
        private final String role;
        private final String content;
    }
}