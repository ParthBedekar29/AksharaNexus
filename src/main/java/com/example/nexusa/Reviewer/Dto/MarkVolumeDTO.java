package com.example.nexusa.Reviewer.Dto;

import lombok.Data;
import java.util.UUID;

@Data
public class MarkVolumeDTO {
    private UUID versionId;
    private String nodeId;
    private String volumeTitle;
    private Long startYear;
    private Long endYear;
    private String reviewerNote;
}