// com/example/nexusa/Repository/ReviewerCodeRepository.java
package com.example.nexusa.Repository;

import com.example.nexusa.Model.ReviewerCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewerCodeRepository extends JpaRepository<ReviewerCode, UUID> {
    Optional<ReviewerCode> findByCodeAndEmail(String code, String email);
}