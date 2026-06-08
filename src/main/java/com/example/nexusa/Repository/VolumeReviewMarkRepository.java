package com.example.nexusa.Repository;

import com.example.nexusa.Model.VolumeReviewMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolumeReviewMarkRepository extends JpaRepository<VolumeReviewMark, UUID> {

    List<VolumeReviewMark> findByReviewer_ReviewerId(UUID reviewerId);

    Optional<VolumeReviewMark> findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
            UUID reviewerId, UUID versionId, String nodeId);

    boolean existsByMarkIdAndReviewer_ReviewerId(UUID markId, UUID reviewerId);

    void deleteByMarkIdAndReviewer_ReviewerId(UUID markId, UUID reviewerId);
}