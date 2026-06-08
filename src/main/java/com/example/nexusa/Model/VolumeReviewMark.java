package com.example.nexusa.Model;

import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Enums.VolumeMarkStatus;
import com.example.nexusa.Model.Reviewer;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "volume_review_marks",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"reviewer_id", "version_id", "node_id"}))
@Data
public class VolumeReviewMark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID markId;

    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Reviewer reviewer;

    @ManyToOne
    @JoinColumn(name = "version_id", nullable = false)
    private CVersion version;

    /** The nodeId of the VOLUME node inside the serialized tree */
    @Column(name = "node_id", nullable = false)
    private String nodeId;

    /** Denormalized for display — pulled from tree data at mark time */
    @Column(name = "volume_title", nullable = false)
    private String volumeTitle;

    @Column(name = "start_year")
    private Long startYear;

    @Column(name = "end_year")
    private Long endYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_status", nullable = false)
    private VolumeMarkStatus markStatus = VolumeMarkStatus.SAVED;

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

    @CreationTimestamp
    @Column(name = "marked_at", updatable = false)
    private LocalDateTime markedAt;
}