package com.example.nexusa.Model.Central;

import com.example.nexusa.Model.Reviewer;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "central_volumes")
@Data
public class CentralVolume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "volume_id")
    private UUID volumeId;

    @ManyToOne
    @JoinColumn(name = "central_civ_id", nullable = false)
    private CentralCivilization civilization;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_year")
    private Long startYear;

    @Column(name = "end_year")
    private Long endYear;

    // Display order within the civilization
    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Reviewer createdBy;
}