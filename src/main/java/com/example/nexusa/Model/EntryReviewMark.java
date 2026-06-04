package com.example.nexusa.Model;

import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Central.CentralCivilization; // 1. Import the CentralCivilization model
import com.example.nexusa.Model.Enums.EntryMarkStatus;
import com.example.nexusa.Model.Reviewer;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entry_review_marks")
@Data
public class EntryReviewMark {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID markId;

    // Which reviewer marked it
    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Reviewer reviewer;

    // Source version containing this entry
    @ManyToOne
    @JoinColumn(name = "version_id", nullable = false)
    private CVersion version;

    // 2. Added Foreign Key link to CentralCivilization
    @ManyToOne
    @JoinColumn(name = "central_civ_id", nullable = false) // Maps to central_civ_id in central_civilizations table
    private CentralCivilization centralCivilization;

    // The node ID within the serialized tree
    @Column(name = "node_id", nullable = false)
    private String nodeId;

    // Entry title (denormalized for display)
    @Column(name = "entry_title")
    private String entryTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_status", nullable = false)
    private EntryMarkStatus markStatus; // APPROVED, REJECTED, REVISION_REQUESTED

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

    @CreationTimestamp
    @Column(name = "marked_at", updatable = false)
    private LocalDateTime markedAt;
}