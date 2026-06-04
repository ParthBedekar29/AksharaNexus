package com.example.nexusa.Reviewer.Dto;


import lombok.Data;
import java.util.UUID;

@Data
public class MarkEntryForCentralDTO {
    private String nodeId;
    private UUID versionId;
    private UUID centralCivId;
    private String entryTitle; // Add this field
}