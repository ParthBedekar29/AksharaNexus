package com.example.nexusa.Repository;

import com.example.nexusa.Model.EntryReviewMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryReviewMarkRepository extends JpaRepository<EntryReviewMark, UUID> {
    List<EntryReviewMark> findByReviewer_ReviewerId(UUID reviewerId);
    List<EntryReviewMark> findByVersion_VersionId(UUID versionId);
    Optional<EntryReviewMark> findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
            UUID reviewerId, UUID versionId, String nodeId);
    boolean existsByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
            UUID reviewerId, UUID versionId, String nodeId);
    void deleteByMarkIdAndReviewer_ReviewerId(UUID markId, UUID reviewerId);
    boolean existsByMarkIdAndReviewer_ReviewerId(UUID markId, UUID reviewerId);
    List<EntryReviewMark> findByVersion_Civilization_CivId(UUID civId);
}