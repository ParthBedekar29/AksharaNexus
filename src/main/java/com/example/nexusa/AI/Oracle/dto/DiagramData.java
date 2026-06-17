package com.example.nexusa.AI.Oracle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured representation of a diagram emitted by the LLM as a fenced
 * ```diagram { ... } ``` JSON block. Parsed out of the markdown answer by
 * OracleService#extractDiagram and rendered natively by the Flutter client
 * instead of being shown as raw JSON text.
 *
 * type:
 *   "process"    — sequential or branching flow (nodes connected by edges)
 *   "hierarchy"  — tree / org-chart / taxonomy (edges imply parent → child)
 *   "comparison" — side-by-side structural comparison (nodes are the two
 *                  subjects' attributes; edges generally unused or used to
 *                  link corresponding attributes)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramData {

    private String type;
    private String title;
    private List<DiagramNode> nodes;
    private List<DiagramEdge> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramNode {
        private String id;
        private String label;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramEdge {
        private String from;
        private String to;
        private String label;
    }
}