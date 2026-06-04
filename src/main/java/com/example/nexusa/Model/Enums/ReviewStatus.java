package com.example.nexusa.Model.Enums;

public enum ReviewStatus {
    DRAFT, PENDING_REVIEW, REVISION_REQUESTED, PUBLISHED, REJECTED,ENTRY_APPROVED,   // reviewer marked this entry for central
    ENTRY_REJECTED,   // reviewer rejected this specific entry
    ENTRY_REVISION_REQUESTED  // reviewer wants changes to this entry
}