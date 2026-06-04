package com.example.nexusa.Model.Central;

import com.example.nexusa.Model.Reviewer;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "central_civilizations")
@Data
public class CentralCivilization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "central_civ_id")
    private UUID centralCivId;

    // Added unique = true to enforce unique civilization titles
    @Column(name = "title", nullable = false, unique = true)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_year")
    private Long startYear;

    @Column(name = "end_year")
    private Long endYear;

    // Reviewer who created this central civilization entry
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Reviewer createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}