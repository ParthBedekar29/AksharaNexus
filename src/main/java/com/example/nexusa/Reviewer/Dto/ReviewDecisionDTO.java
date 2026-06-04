package com.example.nexusa.Reviewer.Dto;

import com.example.nexusa.Model.Enums.ReviewStatus;
import lombok.Data;

@Data
public class ReviewDecisionDTO {
    private ReviewStatus status;  // PUBLISHED, REJECTED, REVISION_REQUESTED only
    private String note;
}