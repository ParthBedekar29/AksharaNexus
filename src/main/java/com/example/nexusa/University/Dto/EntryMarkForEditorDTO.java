package com.example.nexusa.University.Dto;

import com.example.nexusa.Model.Enums.EntryMarkStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EntryMarkForEditorDTO {
    private UUID markId;
    private String nodeId;
    private String entryTitle;
    private EntryMarkStatus markStatus;
    private String reviewerNote;
    private LocalDateTime markedAt;
    private String reviewerName;      // first + last of the Reviewer who marked it
}