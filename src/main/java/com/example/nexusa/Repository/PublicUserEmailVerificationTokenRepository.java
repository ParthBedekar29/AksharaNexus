package com.example.nexusa.Repository;

import com.example.nexusa.Model.PublicUser;
import com.example.nexusa.Model.PublicUserEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PublicUserEmailVerificationTokenRepository extends JpaRepository<PublicUserEmailVerificationToken, UUID> {
    Optional<PublicUserEmailVerificationToken> findByTokenAndUsedFalse(UUID token);
    // PublicUserEmailVerificationTokenRepository
    void deleteAllByPublicUser(PublicUser user);

    // PublicUserPasswordResetTokenRepository

}