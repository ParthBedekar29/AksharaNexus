package com.example.nexusa.Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "civilizations")
@Data
public class Civilization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "civ_id")
    private UUID civId;

    @ManyToOne
    @JoinColumn(name = "uni_id", nullable = false)
    private University university;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "start_date")
    private Long startDate;

    @Column(name = "end_date")
    private Long endDate;
}