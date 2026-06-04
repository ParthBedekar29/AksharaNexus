package com.example.nexusa.Reviewer.Dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BatchAddCentralEntriesDTO {
    private List<AddCentralEntryDTO> entries; // reuse existing DTO
}