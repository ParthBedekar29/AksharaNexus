package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CivMetadataDTO {
    private UUID civId;
    private String title;
    private String description;
    private Long startYear;
    private Long endYear;
    private String universityName;
}