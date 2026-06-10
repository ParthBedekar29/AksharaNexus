package com.example.nexusa.Repository;

import com.example.nexusa.Model.PublicUserPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PublicUserPasswordResetTokenRepository extends JpaRepository<PublicUserPasswordResetToken, UUID> {
    Optional<PublicUserPasswordResetToken> findByTokenAndUsedFalse(UUID token);
}