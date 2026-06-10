package com.example.nexusa.Repository;

import com.example.nexusa.Model.ReviewerPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface ReviewerPasswordResetTokenRepository
        extends JpaRepository<ReviewerPasswordResetToken, UUID> {

    Optional<ReviewerPasswordResetToken> findByToken(UUID token);

    @Transactional
    void deleteByReviewer_ReviewerId(UUID reviewerId);
}