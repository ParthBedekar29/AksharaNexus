package com.example.nexusa.Repository;

import com.example.nexusa.Model.ReviewerEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface ReviewerEmailVerificationTokenRepository
        extends JpaRepository<ReviewerEmailVerificationToken, UUID> {

    Optional<ReviewerEmailVerificationToken> findByToken(UUID token);

    @Transactional
    void deleteByReviewer_ReviewerId(UUID reviewerId);
}