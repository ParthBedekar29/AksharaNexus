package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CentralCivilizationDetailDTO {

    private UUID centralCivId;

    private String title;
    private String description;
    private Long startYear;
    private Long endYear;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private String createdByName;

    private List<CentralVolumeDTO> volumes;

    @Data
    public static class CentralVolumeDTO {
        private UUID volumeId;
        private String title;
        private Long startYear;
        private Long endYear;
        private Integer position;
        private List<CentralEntryDTO> entries;
    }

    @Data
    public static class CentralEntryDTO {
        private UUID centralEntryId;
        private String title;
        private Long startYear;
        private Long endYear;
        private String serializedBlocks;
        private Integer position;
        private Boolean isDivergent;

        // Provenance
        private UUID sourceVersionId;
        private String sourceVersionHash;
        private UUID sourceUniversityId;
        private String sourceUniversityName;
        private String sourceNodeId;

        // Divergence info if flagged
        private List<DivergenceDTO> divergences;
    }

    @Data
    public static class DivergenceDTO {
        private UUID divergenceId;
        private UUID conflictingEntryId;
        private String conflictingEntryTitle;
        private String conflictingUniversityName;
        private String divergenceNote;
    }
}