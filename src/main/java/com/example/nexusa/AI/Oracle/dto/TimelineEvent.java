package com.example.nexusa.AI.Oracle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimelineEvent {
    private String title;
    private String date;
    private String location;
    private String description;
    private Long year; // normalized for sorting
}