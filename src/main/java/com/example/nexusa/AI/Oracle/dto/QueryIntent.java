package com.example.nexusa.AI.Oracle.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryIntent {
    private String civilizationName;
    private String topic;           // "military", "trade", "governance" etc — mapped from keywords
    private Long startYear;
    private Long endYear;
    private List<String> keywords;  // raw extracted keywords for block ranking
    private String rawQuery;
}