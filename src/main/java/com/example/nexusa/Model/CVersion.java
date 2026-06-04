package com.example.nexusa.Model;

import com.example.nexusa.Model.Enums.CommitType;
import com.example.nexusa.Model.Enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "civilization_versions")
@Data
public class CVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne
    @JoinColumn(name = "civ_id", nullable = false)
    private Civilization civilization;

    @Column(name = "hash", nullable = false)
    private String hash;

    @Column(name = "version_title")
    private String versionTitle;

    @Column(name = "serialized_tree", columnDefinition = "TEXT", nullable = false)
    private String serializedTree;

    @ManyToOne
    @JoinColumn(name = "committed_by", nullable = false)
    private User committedBy;

    @Column(name = "commit_message")
    private String commitMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "commit_type")
    private CommitType commitType;

    @Column(name = "commit_timestamp")
    private LocalDateTime commitTimestamp;

    @Column(name = "start_date")
    private Long startDate;

    @Column(name = "end_date")
    private Long endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status")
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    // FK now points to reviewers table, not users
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Reviewer reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;
}