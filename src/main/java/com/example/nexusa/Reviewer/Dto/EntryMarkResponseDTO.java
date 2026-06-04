package com.example.nexusa.Reviewer.Dto;

import com.example.nexusa.Model.Enums.EntryMarkStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EntryMarkResponseDTO {
    private UUID markId;
    private UUID versionId;
    private String nodeId;
    private String entryTitle;
    private EntryMarkStatus markStatus;
    private String reviewerNote;
    private LocalDateTime markedAt;
    // Add to existing EntryMarkResponseDTO
    private String civTitle;
    private String universityName;
    private String committedByName;
    private Long civStartYear;
    private Long civEndYear;
    private Long entryStartYear;
    private Long entryEndYear;
}