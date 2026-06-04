package com.example.nexusa.Repository;

import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVersionRepository extends JpaRepository<CVersion, UUID> {
    List<CVersion> findByCivilization_CivId(UUID civId);
    Optional<CVersion> findByHash(String hash);

    Optional<CVersion> findTopByCivilization_CivIdOrderByCommitTimestampDesc(UUID civId);

    Optional<CVersion> findByCivilization_CivIdAndHash(UUID civId, String hash);
    List<CVersion> findByCivilization_CivIdOrderByCommitTimestampDesc(UUID civId);
    void deleteByCivilization_CivId(UUID civId);

    List<CVersion> findByReviewStatus(ReviewStatus status);

    List<CVersion> findByReviewedBy_ReviewerId(UUID reviewerId);
}