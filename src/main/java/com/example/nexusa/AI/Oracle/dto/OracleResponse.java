package com.example.nexusa.AI.Oracle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OracleResponse {
    private String answer;
    private List<String> sourceCitations;  // pulled from top-ranked blocks
    private String civilizationMatched;
}