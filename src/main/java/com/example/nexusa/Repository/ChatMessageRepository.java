package com.example.nexusa.Repository;

import com.example.nexusa.Model.ChatMessage;
import com.example.nexusa.Model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    void deleteAllBySession(ChatSession session);
}