package com.example.nexusa.Reviewer.Dto;

import com.example.nexusa.Model.Enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VersionSummaryDTO {
    private UUID versionId;
    private String hash;
    private String commitMessage;
    private LocalDateTime commitTimestamp;
    private ReviewStatus reviewStatus;
    private String reviewerNote;
    private LocalDateTime reviewedAt;

    // Civilization info
    private UUID civId;
    private String civTitle;
    private Long civStartDate;
    private Long civEndDate;

    // University info
    private UUID universityId;
    private String universityName;

    // Committer info
    private String committedByName;
}