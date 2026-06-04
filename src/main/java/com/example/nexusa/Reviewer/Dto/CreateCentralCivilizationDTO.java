package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

@Data
// New minimal DTO
// Updated DTO — reviewer picks from university metadata

public class CreateCentralCivilizationDTO {

    private String title;
    private String description;  // chosen from a university's civ
    private Long startYear;      // chosen from a university's civ
    private Long endYear;        // chosen from a university's civ
}
