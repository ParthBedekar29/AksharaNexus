package com.example.nexusa.Repository;

import com.example.nexusa.Model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewerRepository extends JpaRepository<Reviewer, UUID> {
    Optional<Reviewer> findByEmail(String email);
    boolean existsByEmail(String email);
}