package com.example.nexusa.Repository;

import com.example.nexusa.Model.UniversityDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniversityDomainRepository extends JpaRepository<UniversityDomain, UUID> {
    Optional<UniversityDomain> findByDomain(String domain);
}