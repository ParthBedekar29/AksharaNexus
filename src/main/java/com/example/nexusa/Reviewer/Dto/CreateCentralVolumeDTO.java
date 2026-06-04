package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

@Data
public class CreateCentralVolumeDTO {
    private String title;
    private Long startYear;
    private Long endYear;
    private Integer position;
}