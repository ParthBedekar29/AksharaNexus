package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CentralCivilizationSummaryDTO {
    private UUID centralCivId;

    private String title;
    private String description;
    private Long startYear;
    private Long endYear;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private String createdByName;
    private Integer volumeCount;
    private Integer entryCount;
}