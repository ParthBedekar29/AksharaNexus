package com.example.nexusa.University.Service;

import com.example.nexusa.Repository.*;
import com.example.nexusa.University.Dto.AddNodeRequestDTO;
import com.example.nexusa.University.Dto.CreateCivilizationDTO;
import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Civilization;
import com.example.nexusa.Model.EditorAssignment;
import com.example.nexusa.Model.Enums.ReviewStatus;
import com.example.nexusa.Model.Enums.Role;
import com.example.nexusa.Model.User;
import com.example.nexusa.University.Dto.EntryMarkForEditorDTO;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CivilizationService {
    private final UserRepository userRepository;
    private final CivilizationRepository civilizationRepository;
    private final CVersionRepository cVersionRepository;
    private final CMETreeService cmeTreeService;
    private final EditorAssignmentRepository editorAssignmentRepository;
    private final EntryReviewMarkRepository entryReviewMarkRepository;

    public CivilizationService(UserRepository userRepository,
                               CivilizationRepository civilizationRepository,
                               CVersionRepository cVersionRepository,
                               CMETreeService cmeTreeService,
                               EditorAssignmentRepository editorAssignmentRepository, EntryReviewMarkRepository entryReviewMarkRepository) {
        this.userRepository = userRepository;
        this.civilizationRepository = civilizationRepository;
        this.cVersionRepository = cVersionRepository;
        this.cmeTreeService = cmeTreeService;
        this.editorAssignmentRepository = editorAssignmentRepository;
        this.entryReviewMarkRepository = entryReviewMarkRepository;
    }

    private User getAuthenticatedUser() {
        String mail = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return userRepository.findUserByEmail(mail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void assertIsEditor(User user, UUID civId) {
        if (user.getRole() == Role.ADMIN) return;
        if (!editorAssignmentRepository.existsByCivilization_CivIdAndEditor_UserId(civId, user.getUserId())) {
            throw new RuntimeException("You are not an editor for this civilization");
        }
    }

    public UUID createCivilization(CreateCivilizationDTO createCivilizationDTO) {
        User uniAdmin = getAuthenticatedUser();
        if (uniAdmin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can create civilizations");
        }
        Civilization civilization = new Civilization();
        civilization.setTitle(createCivilizationDTO.getTitle());
        civilization.setDescription(createCivilizationDTO.getDescription());
        civilization.setCreatedBy(uniAdmin);
        civilization.setStartDate(createCivilizationDTO.getStartYear());
        civilization.setEndDate(createCivilizationDTO.getEndYear());
        civilization.setUniversity(uniAdmin.getUniID());
        civilizationRepository.save(civilization);
        try {
            CVersion initialVersion = cmeTreeService.createInitialVersion(civilization, createCivilizationDTO.getCommitMsg(), uniAdmin);
            cVersionRepository.save(initialVersion);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return civilization.getCivId();
    }
    public void submitForReview(UUID civId, UUID versionId) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN)
            throw new RuntimeException("Only admins can submit for review");

        CVersion version = cVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        if (!version.getCivilization().getCivId().equals(civId))
            throw new RuntimeException("Version does not belong to this civilization");
        if (version.getReviewStatus() != ReviewStatus.DRAFT &&
                version.getReviewStatus() != ReviewStatus.REVISION_REQUESTED)
            throw new RuntimeException("Only DRAFT or REVISION_REQUESTED versions can be submitted");

        version.setReviewStatus(ReviewStatus.PENDING_REVIEW);
        cVersionRepository.save(version);
    }


    public List<EntryMarkForEditorDTO> getEntryMarksForCiv(UUID civId) {
        User user = getAuthenticatedUser();
        // Editors and admins of this civ can see marks
        if (user.getRole() != Role.ADMIN) {
            if (!editorAssignmentRepository.existsByCivilization_CivIdAndEditor_UserId(
                    civId, user.getUserId()))
                throw new RuntimeException("Not authorized");
        }

        return entryReviewMarkRepository.findByVersion_Civilization_CivId(civId)
                .stream()
                .map(m -> {
                    EntryMarkForEditorDTO dto = new EntryMarkForEditorDTO();
                    dto.setMarkId(m.getMarkId());
                    dto.setNodeId(m.getNodeId());
                    dto.setEntryTitle(m.getEntryTitle());
                    dto.setMarkStatus(m.getMarkStatus());
                    dto.setReviewerNote(m.getReviewerNote());
                    dto.setMarkedAt(m.getMarkedAt());
                    if (m.getReviewer() != null)
                        dto.setReviewerName(m.getReviewer().getFirstName()
                                + " " + m.getReviewer().getLastName());
                    return dto;
                }).toList();
    }

    public List<User> getUniversityUsers() {
        User admin = getAuthenticatedUser();
        return userRepository.findByUniID(admin.getUniID());
    }

    public CVersion getLatestVersion(UUID civId) {
        return cVersionRepository.findTopByCivilization_CivIdOrderByCommitTimestampDesc(civId)
                .orElseThrow(() -> new RuntimeException("No versions found"));
    }

    public void assignEditor(UUID civId, UUID userId) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can assign editors");
        }
        Civilization civ = civilizationRepository.findById(civId)
                .orElseThrow(() -> new RuntimeException("Civilization not found"));
        User editor = userRepository.findUserByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (editorAssignmentRepository.existsByCivilization_CivIdAndEditor_UserId(civId, userId)) {
            throw new RuntimeException("User already an editor for this civilization");
        }
        EditorAssignment assignment = new EditorAssignment();
        assignment.setCivilization(civ);
        assignment.setEditor(editor);
        editor.setRole(Role.EDITOR);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(LocalDateTime.now());
        editorAssignmentRepository.save(assignment);
    }
    @Transactional
    public void deleteCivilization(UUID civId) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can delete civilizations");
        }
        Civilization civ = civilizationRepository.findById(civId)
                .orElseThrow(() -> new RuntimeException("Civilization not found"));

        // Compare University IDs, not object references
        if (!civ.getUniversity().getId().equals(admin.getUniID().getId())) {
            throw new RuntimeException("Civilization does not belong to your university");
        }

        editorAssignmentRepository.deleteByCivilization_CivId(civId);
        cVersionRepository.deleteByCivilization_CivId(civId);
        civilizationRepository.deleteById(civId);
    }
    public List<EditorAssignment> getCivilizationEditors(UUID civId) {
        return editorAssignmentRepository.findByCivilization_CivId(civId);
    }

    public List<Civilization> getMyCivilizations() {
        User editor = getAuthenticatedUser();
        return editorAssignmentRepository.findByEditor_UserId(editor.getUserId())
                .stream()
                .map(EditorAssignment::getCivilization)
                .toList();
    }

    public CVersion addVolume(UUID civId, AddNodeRequestDTO dto) {
        User user = getAuthenticatedUser();
        assertIsEditor(user, civId);
        CVersion version = cmeTreeService.addVolume(civId, dto.getParentNodeId(), dto.getNode(), dto.getCommitMsg(), user);
        return cVersionRepository.save(version);
    }

    public CVersion addEntry(UUID civId, AddNodeRequestDTO dto) {
        User user = getAuthenticatedUser();
        assertIsEditor(user, civId);
        CVersion version = cmeTreeService.addEntry(civId, dto.getParentNodeId(), dto.getNode(), dto.getCommitMsg(), user);
        return cVersionRepository.save(version);
    }

    public CVersion updateNode(UUID civId, UUID nodeId, AddNodeRequestDTO dto) {
        User user = getAuthenticatedUser();
        assertIsEditor(user, civId);
        CVersion version = cmeTreeService.updateEntry(civId, nodeId, dto.getNode(), dto.getCommitMsg(), user);
        return cVersionRepository.save(version);
    }

    public CVersion rollback(UUID civId, String hash) {
        User user = getAuthenticatedUser();
        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can rollback");
        }
        CVersion version = cmeTreeService.rollback(civId, hash, user);
        return cVersionRepository.save(version);
    }

    public List<CVersion> getAllVersions(UUID civId) {
        return cVersionRepository.findByCivilization_CivIdOrderByCommitTimestampDesc(civId);
    }

    // In CivilizationService.java
    public List<Civilization> getAllUniversityCivilizations() {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can access this");
        }
        return civilizationRepository.findByUniversity(admin.getUniID());
    }


}