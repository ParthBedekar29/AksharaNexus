package com.example.nexusa.Model.Central;

import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Reviewer;
import com.example.nexusa.Model.University;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "central_entries")
@Data
public class CentralEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "central_entry_id")
    private UUID centralEntryId;

    @ManyToOne
    @JoinColumn(name = "volume_id", nullable = false)
    private CentralVolume volume;

    // ── Provenance ────────────────────────────────────────────────────────────

    // The exact civilization_versions row this entry was sourced from
    @ManyToOne
    @JoinColumn(name = "source_version_id", nullable = false)
    private CVersion sourceVersion;

    // The university whose tree this entry came from
    @ManyToOne
    @JoinColumn(name = "source_university_id", nullable = false)
    private University sourceUniversity;

    // The exact nodeId string from the serialized tree JSON
    // Used to locate and extract the entry's data at query time
    @Column(name = "source_node_id", nullable = false)
    private String sourceNodeId;

    // ── Entry metadata (denormalized from tree for query performance) ─────────

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_year")
    private Long startYear;

    @Column(name = "end_year")
    private Long endYear;

    // Full serialized blocks JSON for this entry, extracted from source tree
    // Stored here so queries don't need to deserialize the entire civilization tree
    @Column(name = "serialized_blocks", columnDefinition = "TEXT")
    private String serializedBlocks;

    // Display order within the volume
    @Column(name = "position", nullable = false)
    private Integer position;

    // True if another university has a conflicting entry on the same topic
    @Column(name = "is_divergent", nullable = false)
    private Boolean isDivergent = false;

    @ManyToOne
    @JoinColumn(name = "added_by", nullable = false)
    private Reviewer addedBy;
}