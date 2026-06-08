package com.example.nexusa.Reviewer.Dto;

import com.example.nexusa.Model.Enums.VolumeMarkStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VolumeMarkResponseDTO {
    private UUID markId;
    private UUID versionId;
    private String nodeId;
    private String volumeTitle;
    private Long startYear;
    private Long endYear;
    private VolumeMarkStatus markStatus;
    private String reviewerNote;
    private LocalDateTime markedAt;

    // Enriched context
    private String civTitle;
    private String universityName;
    private String committedByName;
}