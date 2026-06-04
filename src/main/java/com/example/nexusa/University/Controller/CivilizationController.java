package com.example.nexusa.University.Controller;

import com.example.nexusa.University.Dto.AddNodeRequestDTO;
import com.example.nexusa.University.Dto.CreateCivilizationDTO;

import com.example.nexusa.University.Dto.EntryMarkForEditorDTO;
import com.example.nexusa.University.Dto.RollbackRequestDTO;
import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Civilization;
import com.example.nexusa.Model.EditorAssignment;
import com.example.nexusa.Model.User;
import com.example.nexusa.University.Service.CivilizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class CivilizationController {
    private final CivilizationService civilizationService;

    public CivilizationController(CivilizationService civilizationService) {
        this.civilizationService = civilizationService;
    }

    @GetMapping("/civilization/users")
    public ResponseEntity<List<User>> getUniversityUsers() {
        try {
            return ResponseEntity.ok(civilizationService.getUniversityUsers());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/civilization/create")
    public ResponseEntity<UUID> createCivilization(@RequestBody CreateCivilizationDTO dto) {
        try {
            return ResponseEntity.ok(civilizationService.createCivilization(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @DeleteMapping("/civilization/{civId}")
    public ResponseEntity<String> deleteCivilization(@PathVariable UUID civId) {
        try {
            civilizationService.deleteCivilization(civId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/civilization/{civId}/entry-marks")
    public ResponseEntity<List<EntryMarkForEditorDTO>> getEntryMarks(@PathVariable UUID civId) {
        try {
            return ResponseEntity.ok(civilizationService.getEntryMarksForCiv(civId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/civilization/{civId}/latest")
    public ResponseEntity<CVersion> getLatestVersion(@PathVariable UUID civId) {
        try {
            return ResponseEntity.ok(civilizationService.getLatestVersion(civId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }@PatchMapping("/civilization/{civId}/version/{versionId}/submit")
    public ResponseEntity<String> submitForReview(@PathVariable UUID civId,
                                                  @PathVariable UUID versionId) {
        try {
            civilizationService.submitForReview(civId, versionId);
            return ResponseEntity.ok("Submitted for review");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }




    @GetMapping("/civilization/{civId}/versions")
    public ResponseEntity<List<CVersion>> getAllVersions(@PathVariable UUID civId) {
        try {
            return ResponseEntity.ok(civilizationService.getAllVersions(civId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/civilization/{civId}/editors")
    public ResponseEntity<String> assignEditor(@PathVariable UUID civId, @RequestBody Map<String, UUID> body) {
        try {
            civilizationService.assignEditor(civId, body.get("userId"));
            return ResponseEntity.ok("Editor assigned");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/civilization/{civId}/editors")
    public ResponseEntity<List<EditorAssignment>> getCivilizationEditors(@PathVariable UUID civId) {
        try {
            return ResponseEntity.ok(civilizationService.getCivilizationEditors(civId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/civilization/my")
    public ResponseEntity<List<Civilization>> getMyCivilizations() {
        try {
            return ResponseEntity.ok(civilizationService.getMyCivilizations());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/civilization/{civId}/volume")
    public ResponseEntity<CVersion> addVolume(@PathVariable UUID civId,
                                              @RequestBody AddNodeRequestDTO dto) {
        try {
            return ResponseEntity.ok(civilizationService.addVolume(civId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/civilization/{civId}/entry")
    public ResponseEntity<CVersion> addEntry(@PathVariable UUID civId,
                                             @RequestBody AddNodeRequestDTO dto) {
        try {
            return ResponseEntity.ok(civilizationService.addEntry(civId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/civilization/{civId}/node/{nodeId}")
    public ResponseEntity<CVersion> updateNode(@PathVariable UUID civId,
                                               @PathVariable UUID nodeId,
                                               @RequestBody AddNodeRequestDTO dto) {
        try {
            return ResponseEntity.ok(civilizationService.updateNode(civId, nodeId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/civilization/{civId}/rollback")
    public ResponseEntity<CVersion> rollback(@PathVariable UUID civId,
                                             @RequestBody RollbackRequestDTO dto) {
        try {
            return ResponseEntity.ok(civilizationService.rollback(civId, dto.getHash()));
        }catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().build();
    }
    }
    // In CivilizationController.java
    @GetMapping("/civilization/all")
    public ResponseEntity<List<Civilization>> getAllUniversityCivilizations() {
        try {
            return ResponseEntity.ok(civilizationService.getAllUniversityCivilizations());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}