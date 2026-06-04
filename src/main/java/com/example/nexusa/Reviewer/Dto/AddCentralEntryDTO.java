package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddCentralEntryDTO {
    private UUID sourceVersionId;   // which civilization_versions row
    private String sourceNodeId;    // exact nodeId from the serialized tree JSON
    private Integer position;       // where in the volume
}