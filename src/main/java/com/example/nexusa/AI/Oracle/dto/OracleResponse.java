package com.example.nexusa.AI.Oracle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class OracleResponse {
    private String answer;
    private List<String> sourceCitations;
    private String civilizationMatched;
    private List<TimelineEvent> timeline;   // null for non-timeline queries
    private DiagramData diagram;            // null when no diagram was produced
}