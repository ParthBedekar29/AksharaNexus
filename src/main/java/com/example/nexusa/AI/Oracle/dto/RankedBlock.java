package com.example.nexusa.AI.Oracle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RankedBlock {
    private String entryTitle;
    private String volumeTitle;
    private String blockType;       // TEXT, EVENT, ASPECT
    private String formattedContent; // human-readable string built from block JSON
    private List<String> citationSummaries;
    private double relevanceScore;
}