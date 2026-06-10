package com.example.nexusa.Repository;

import com.example.nexusa.Model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(UUID token);

    @Transactional
    void deleteByUser_UserId(UUID userId);
}