package com.example.nexusa.Model;

import com.example.nexusa.Model.Enums.CitationQuality;
import lombok.Data;

@Data
public class Citation {
    private String source;
    private String author;
    private Integer year;
    private String url;
    private CitationQuality quality;        // editor-assigned
    private CitationQuality reviewedQuality; // reviewer override, null until reviewed
    private Boolean verified;               // reviewer confirmed source exists and says what's claimed
}