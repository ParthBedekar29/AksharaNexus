package com.example.nexusa.Model.Central;

import com.example.nexusa.Model.Reviewer;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "central_entry_divergences")
@Data
public class CentralEntryDivergence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "divergence_id")
    private UUID divergenceId;

    // The primary entry that was already in the central tree
    @ManyToOne
    @JoinColumn(name = "primary_entry_id", nullable = false)
    private CentralEntry primaryEntry;

    // The conflicting entry from a different university
    @ManyToOne
    @JoinColumn(name = "conflicting_entry_id", nullable = false)
    private CentralEntry conflictingEntry;

    // Reviewer's note explaining the nature of the disagreement
    // e.g. "Oxford dates collapse to 1900 BCE, Delhi argues 1700 BCE"
    @Column(name = "divergence_note", columnDefinition = "TEXT")
    private String divergenceNote;

    @ManyToOne
    @JoinColumn(name = "flagged_by", nullable = false)
    private Reviewer flaggedBy;

    @CreationTimestamp
    @Column(name = "flagged_at", updatable = false)
    private LocalDateTime flaggedAt;
}