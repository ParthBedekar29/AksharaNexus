package com.example.nexusa.Repository;

import com.example.nexusa.Model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(UUID token);
    void deleteByUser_UserId(UUID userId);
}