package com.example.nexusa.Reviewer.Controller;

import com.example.nexusa.Reviewer.Dto.*;
import com.example.nexusa.Reviewer.Service.CentralService;
import com.example.nexusa.Reviewer.Service.ReviewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviewer")
public class ReviewerController {

    private final ReviewerService reviewerService;
    private final CentralService centralService;

    public ReviewerController(ReviewerService reviewerService,
                              CentralService centralService) {
        this.reviewerService = reviewerService;
        this.centralService  = centralService;
    }

    // ── Civilization browse (latest version per civ, all universities) ─────────

    @GetMapping("/civilizations/latest")
    public ResponseEntity<List<VersionDetailDTO>> getAllLatestVersions() {
        try {
            return ResponseEntity.ok(reviewerService.getAllLatestVersions());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/version/{versionId}")
    public ResponseEntity<VersionDetailDTO> getVersionDetail(@PathVariable UUID versionId) {
        try {
            return ResponseEntity.ok(reviewerService.getVersionDetail(versionId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
// ── Delete entry from central ─────────────────────────────────────────────

    // ── Delete entry from central ─────────────────────────────────────────────
    @DeleteMapping("/central/{centralCivId}/volume/{volumeId}/entry/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable UUID centralCivId,
                                            @PathVariable UUID volumeId,
                                            @PathVariable UUID centralEntryId) {
        try {
            centralService.deleteEntry(centralCivId, volumeId, centralEntryId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/central/{centralCivId}/volume/{volumeId}")
    public ResponseEntity<Void> deleteVolume(@PathVariable UUID centralCivId,
                                             @PathVariable UUID volumeId) {
        try {
            centralService.deleteVolume(centralCivId, volumeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/central/{centralCivId}/volume/{volumeId}/entries/batch")
    public ResponseEntity<BatchAddResultDTO> addEntriesBatch(
            @PathVariable UUID centralCivId,
            @PathVariable UUID volumeId,
            @RequestBody BatchAddCentralEntriesDTO dto) {
        try {
            return ResponseEntity.ok(centralService.addEntriesBatch(centralCivId, volumeId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @DeleteMapping("/entry/mark/{markId}")
    public ResponseEntity<Void> deleteMark(@PathVariable UUID markId) {
        try {
            reviewerService.deleteMark(markId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // ── Entry marking (per-entry approve / reject / revision) ────────────────

    @PostMapping("/entry/mark")
    public ResponseEntity<UUID> markEntry(@RequestBody MarkEntryDTO dto) {
        try {
            return ResponseEntity.ok(reviewerService.markEntry(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/entry/marks")
    public ResponseEntity<List<EntryMarkResponseDTO>> getMyMarks() {
        try {
            return ResponseEntity.ok(reviewerService.getMyMarks());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/version/{versionId}/marks")
    public ResponseEntity<List<EntryMarkResponseDTO>> getVersionMarks(
            @PathVariable UUID versionId) {
        try {
            return ResponseEntity.ok(reviewerService.getMarksForVersion(versionId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Central civilization ──────────────────────────────────────────────────

    @GetMapping("/central/civilizations")
    public ResponseEntity<List<CentralCivilizationSummaryDTO>> getAllCentral() {
        try {
            return ResponseEntity.ok(centralService.getAllCentralCivilizations());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/central/civ-metadata")
    public ResponseEntity<List<CivMetadataDTO>> getCivMetadata(@RequestParam String title) {
        try {
            return ResponseEntity.ok(centralService.getCivMetadataByTitle(title));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/central/{centralCivId}")
    public ResponseEntity<CentralCivilizationDetailDTO> getCentralDetail(
            @PathVariable UUID centralCivId) {
        try {
            return ResponseEntity.ok(centralService.getCentralCivilizationDetail(centralCivId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/central/civilization")
    public ResponseEntity<UUID> createCentralCivilization(
            @RequestBody CreateCentralCivilizationDTO dto) {
        try {
            return ResponseEntity.ok(centralService.createCentralCivilization(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/central/{centralCivId}/volume")
    public ResponseEntity<UUID> addVolume(@PathVariable UUID centralCivId,
                                          @RequestBody CreateCentralVolumeDTO dto) {
        try {
            return ResponseEntity.ok(centralService.addVolume(centralCivId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/central/{centralCivId}/volume/{volumeId}/entry")
    public ResponseEntity<UUID> addEntry(@PathVariable UUID centralCivId,
                                         @PathVariable UUID volumeId,
                                         @RequestBody AddCentralEntryDTO dto) {
        try {
            return ResponseEntity.ok(centralService.addEntry(centralCivId, volumeId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
// ── Central civilization ──────────────────────────────────────────────────

    @GetMapping("/central/civilizations/my-registry")
    public ResponseEntity<List<CentralCivilizationSummaryDTO>> getMyRegistryCivilizations() {
        try {
            return ResponseEntity.ok(centralService.getCentralCivilizationsForCurrentReviewer());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/central/{centralCivId}/divergence")
    public ResponseEntity<String> flagDivergence(@PathVariable UUID centralCivId,
                                                 @RequestBody FlagDivergenceDTO dto) {
        try {
            centralService.flagDivergence(centralCivId, dto);
            return ResponseEntity.ok("Divergence flagged");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // ── Publish & Link Entry Marking to Central Timeline ──────────────────────
    @PostMapping("/entry/mark-for-central")
    public ResponseEntity<?> markEntryForCentral(@RequestBody MarkEntryForCentralDTO dto) {
        try {
            UUID markId = reviewerService.markEntryForCentral(dto);
            return ResponseEntity.ok(markId);
        } catch (Exception e) {
            // FIX: Pass e.getMessage() back to the frontend so it registers as an explicit error text payload
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}