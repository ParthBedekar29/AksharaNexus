package com.example.nexusa.Repository;

import com.example.nexusa.Model.ChatSession;
import com.example.nexusa.Model.PublicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByPublicUserOrderByUpdatedAtDesc(PublicUser user);
    void deleteAllByPublicUser(PublicUser user);
}