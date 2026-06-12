package com.example.nexusa.Repository;

import com.example.nexusa.Model.ChatSession;
import com.example.nexusa.Model.PublicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByPublicUserOrderByUpdatedAtDesc(PublicUser user);
    void deleteAllByPublicUser(PublicUser user);
    @Modifying
    @Query("DELETE FROM ChatSession s WHERE s.id = :id")
    void deleteById(@Param("id") UUID id);
}