package com.example.nexusa.Reviewer.Dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BatchAddResultDTO {
    private List<UUID> addedEntryIds;
    private List<BatchAddResultDTO.FailedEntry> failures;

    @Data
    public static class FailedEntry {
        private String nodeId;
        private String reason;
    }
}