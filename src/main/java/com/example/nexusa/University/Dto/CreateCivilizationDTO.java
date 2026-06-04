package com.example.nexusa.University.Dto;

import lombok.Data;

@Data
public class CreateCivilizationDTO {
    private String title;
    private String description;
    private Long startYear;
    private Long endYear;
    private String commitMsg;
}