package com.example.nexusa.Reviewer.Dto;

import com.example.nexusa.Model.Enums.EntryMarkStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkEntryDTO {
    private UUID versionId;
    private String nodeId;
    private String entryTitle;
    private EntryMarkStatus markStatus; // APPROVED, REJECTED, REVISION_REQUESTED
    private String reviewerNote;
}