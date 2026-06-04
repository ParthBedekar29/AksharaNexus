package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

import java.util.UUID;

@Data
public class FlagDivergenceDTO {
    private UUID primaryEntryId;      // entry already in central
    private UUID conflictingEntryId;  // the other entry that disagrees
    private String divergenceNote;    // reviewer explains the disagreement
}