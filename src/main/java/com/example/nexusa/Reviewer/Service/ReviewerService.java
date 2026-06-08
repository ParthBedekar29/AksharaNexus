// com/example/nexusa/Reviewer/Service/ReviewerService.java
package com.example.nexusa.Reviewer.Service;

import com.example.nexusa.Model.*;
import com.example.nexusa.Model.Central.CentralCivilization;
import com.example.nexusa.Model.Enums.EntryMarkStatus;
import com.example.nexusa.Model.Enums.ReviewStatus;
import com.example.nexusa.Model.Enums.VolumeMarkStatus;
import com.example.nexusa.Repository.*;
import com.example.nexusa.Repository.Central.CentralCivilizationRepository;
import com.example.nexusa.Reviewer.Config.ReviewerJwtService;
import com.example.nexusa.Reviewer.Dto.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewerService {

    private final ReviewerRepository reviewerRepository;
    private final ReviewerCodeRepository reviewerCodeRepository;
    private final CVersionRepository cVersionRepository;
    private final ReviewerJwtService reviewerJwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CivilizationRepository civilizationRepository;
    private final EntryReviewMarkRepository entryReviewMarkRepository;
    private final CentralCivilizationRepository centralCivRepo;
    private final VolumeReviewMarkRepository volumeReviewMarkRepository;

    public ReviewerService(ReviewerRepository reviewerRepository,
                           ReviewerCodeRepository reviewerCodeRepository,
                           CVersionRepository cVersionRepository,
                           ReviewerJwtService reviewerJwtService, CivilizationRepository civilizationRepository, EntryReviewMarkRepository entryReviewMarkRepository, CentralCivilizationRepository centralCivRepo, VolumeReviewMarkRepository volumeReviewMarkRepository) {
        this.reviewerRepository     = reviewerRepository;
        this.reviewerCodeRepository = reviewerCodeRepository;
        this.cVersionRepository     = cVersionRepository;
        this.reviewerJwtService     = reviewerJwtService;
        this.civilizationRepository = civilizationRepository;
        this.entryReviewMarkRepository = entryReviewMarkRepository;
        this.centralCivRepo = centralCivRepo;
        this.volumeReviewMarkRepository = volumeReviewMarkRepository;
        this.passwordEncoder        = new BCryptPasswordEncoder();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Transactional
    public void register(ReviewerRegisterDTO dto) {
        // 1. Look up a code row that matches BOTH the code value AND the email
        ReviewerCode reviewerCode = reviewerCodeRepository
                .findByCodeAndEmail(dto.getCode(), dto.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or unrecognized registration code for this email"));

        // 2. Make sure the code hasn't been used already
        if (Boolean.TRUE.equals(reviewerCode.getUsed()))
            throw new RuntimeException("This registration code has already been used");

        // 3. Make sure the email isn't already registered
        if (reviewerRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("An account with this email already exists");

        // 4. Create the reviewer
        Reviewer reviewer = new Reviewer();
        reviewer.setEmail(dto.getEmail());
        reviewer.setPassword(passwordEncoder.encode(dto.getPassword()));
        reviewer.setFirstName(dto.getFirstName());
        reviewer.setLastName(dto.getLastName());
        reviewerRepository.save(reviewer);

        // 5. Mark the code as used so it can't be reused
        reviewerCode.setUsed(true);
        reviewerCodeRepository.save(reviewerCode);
    }

    public String login(ReviewerLoginDTO dto) {
        Reviewer reviewer = reviewerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), reviewer.getPassword()))
            throw new RuntimeException("Invalid credentials");

        return reviewerJwtService.generateToken(reviewer.getEmail(), "REVIEWER");
    }
    @Transactional
    public UUID markEntryForCentral(MarkEntryForCentralDTO dto) {
        // 1. Get authenticated reviewer context cleanly via helper
        Reviewer reviewer = getAuthenticatedReviewer();

        // 2. Resolve the destination central civilization
        CentralCivilization centralCiv = centralCivRepo.findById(dto.getCentralCivId())
                .orElseThrow(() -> new RuntimeException("Target central civilization registry not found ID: " + dto.getCentralCivId()));

        // 3. Resolve the version entity context
        CVersion version = cVersionRepository.findById(dto.getVersionId())
                .orElseThrow(() -> new RuntimeException("Source history version mismatch ID: " + dto.getVersionId()));

        // 4. Check if a marking row already exists
        Optional<EntryReviewMark> existingMarkOpt = entryReviewMarkRepository
                .findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
                        reviewer.getReviewerId(), dto.getVersionId(), dto.getNodeId());

        EntryReviewMark mark;
        if (existingMarkOpt.isPresent()) {
            mark = existingMarkOpt.get();
        } else {
            mark = new EntryReviewMark();
            mark.setReviewer(reviewer);
            mark.setVersion(version);
            mark.setNodeId(dto.getNodeId());
            mark.setEntryTitle(dto.getEntryTitle()); // Or pull from tree layout if using jackson mapper
        }

        // 5. Update status to APPROVED and link the foreign key mapping container
        mark.setMarkStatus(EntryMarkStatus.APPROVED);
        mark.setCentralCivilization(centralCiv);
        mark.setReviewerNote("Published explicitly into Central Registry Timeline matrix.");

        // FIX: Force JPA to flush state changes immediately to disk within the transactional block
        EntryReviewMark savedMark = entryReviewMarkRepository.saveAndFlush(mark);
        return savedMark.getMarkId();
    }
    // ── Review queue ──────────────────────────────────────────────────────────
// Mark/update an entry's review status
    @Transactional
    public UUID markEntry(MarkEntryDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CVersion version = cVersionRepository.findById(dto.getVersionId())
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // Verify node exists in tree
        // (reuse CentralService's findNode logic or inline it)

        // Upsert — if already marked, update it
        EntryReviewMark mark = entryReviewMarkRepository
                .findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
                        reviewer.getReviewerId(), dto.getVersionId(), dto.getNodeId())
                .orElse(new EntryReviewMark());

        mark.setReviewer(reviewer);
        mark.setVersion(version);
        mark.setNodeId(dto.getNodeId());
        mark.setEntryTitle(dto.getEntryTitle());
        mark.setMarkStatus(dto.getMarkStatus());
        mark.setReviewerNote(dto.getReviewerNote());
        entryReviewMarkRepository.save(mark);
        return mark.getMarkId();
    }

    // Get all marks by this reviewer (for their workspace)
    public List<EntryMarkResponseDTO> getMyMarks() {
        Reviewer reviewer = getAuthenticatedReviewer();
        return entryReviewMarkRepository
                .findByReviewer_ReviewerId(reviewer.getReviewerId())
                .stream().map(this::toMarkDTO).toList();
    }

    // Get marks for a specific version (for display in tree)
    public List<EntryMarkResponseDTO> getMarksForVersion(UUID versionId) {
        return entryReviewMarkRepository
                .findByVersion_VersionId(versionId)
                .stream().map(this::toMarkDTO).toList();
    }

    private EntryMarkResponseDTO toMarkDTO(EntryReviewMark m) {
        EntryMarkResponseDTO dto = new EntryMarkResponseDTO();
        dto.setMarkId(m.getMarkId());
        dto.setVersionId(m.getVersion().getVersionId());
        dto.setNodeId(m.getNodeId());
        dto.setEntryTitle(m.getEntryTitle());
        dto.setMarkStatus(m.getMarkStatus());
        dto.setReviewerNote(m.getReviewerNote());
        dto.setMarkedAt(m.getMarkedAt());

        // Populate enriched context from version → civilization → university
        if (m.getVersion() != null && m.getVersion().getCivilization() != null) {
            var civ = m.getVersion().getCivilization();
            dto.setCivTitle(civ.getTitle());
            dto.setCivStartYear(civ.getStartDate());
            dto.setCivEndYear(civ.getEndDate());
            if (civ.getUniversity() != null)
                dto.setUniversityName(civ.getUniversity().getName());
        }
        if (m.getVersion().getCommittedBy() != null) {
            var u = m.getVersion().getCommittedBy();
            dto.setCommittedByName(u.getFirstName() + " " + u.getLastName());
        }

        return dto;
    }
    @Transactional
    public void deleteMark(UUID markId) {
        Reviewer reviewer = getAuthenticatedReviewer();
        if (!entryReviewMarkRepository.existsByMarkIdAndReviewer_ReviewerId(
                markId, reviewer.getReviewerId()))
            throw new RuntimeException("Mark not found or does not belong to you");
        entryReviewMarkRepository.deleteByMarkIdAndReviewer_ReviewerId(
                markId, reviewer.getReviewerId());
    }
    // In ReviewerService.java — replace getPendingVersions()
    public List<VersionDetailDTO> getAllLatestVersions() {
        // Get all civilizations, find latest version for each
        return civilizationRepository.findAll()
                .stream()
                .map(civ -> cVersionRepository
                        .findTopByCivilization_CivIdOrderByCommitTimestampDesc(civ.getCivId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(this::toDetailDTO)
                .toList();
    }

    public List<VersionSummaryDTO> getMyReviewedVersions() {
        Reviewer reviewer = getAuthenticatedReviewer();
        return cVersionRepository.findByReviewedBy_ReviewerId(reviewer.getReviewerId())
                .stream().map(this::toSummaryDTO).toList();
    }

    public VersionDetailDTO getVersionDetail(UUID versionId) {
        CVersion v = cVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return toDetailDTO(v);
    }

    @Transactional
    public void reviewVersion(UUID versionId, ReviewDecisionDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        ReviewStatus decision = dto.getStatus();
        if (decision == ReviewStatus.DRAFT || decision == ReviewStatus.PENDING_REVIEW)
            throw new RuntimeException("Invalid decision: " + decision);

        CVersion version = cVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        if (version.getReviewStatus() != ReviewStatus.PENDING_REVIEW)
            throw new RuntimeException("Version is not pending review");

        if ((decision == ReviewStatus.REJECTED || decision == ReviewStatus.REVISION_REQUESTED)
                && (dto.getNote() == null || dto.getNote().isBlank()))
            throw new RuntimeException("A note is required for rejections and revision requests");

        version.setReviewStatus(decision);
        version.setReviewedBy(reviewer);
        version.setReviewedAt(LocalDateTime.now());
        version.setReviewerNote(dto.getNote());
        cVersionRepository.save(version);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    public VersionSummaryDTO toSummaryDTO(CVersion v) {
        VersionSummaryDTO dto = new VersionSummaryDTO();
        dto.setVersionId(v.getVersionId());
        dto.setHash(v.getHash());
        dto.setCommitMessage(v.getCommitMessage());
        dto.setCommitTimestamp(v.getCommitTimestamp());
        dto.setReviewStatus(v.getReviewStatus());
        dto.setReviewerNote(v.getReviewerNote());
        dto.setReviewedAt(v.getReviewedAt());

        if (v.getCivilization() != null) {
            dto.setCivId(v.getCivilization().getCivId());
            dto.setCivTitle(v.getCivilization().getTitle());
            dto.setCivStartDate(v.getCivilization().getStartDate());
            dto.setCivEndDate(v.getCivilization().getEndDate());
            if (v.getCivilization().getUniversity() != null) {
                dto.setUniversityId(v.getCivilization().getUniversity().getId());
                dto.setUniversityName(v.getCivilization().getUniversity().getName());
            }
        }

        if (v.getCommittedBy() != null)
            dto.setCommittedByName(v.getCommittedBy().getFirstName()
                    + " " + v.getCommittedBy().getLastName());

        return dto;
    }

    public VersionDetailDTO toDetailDTO(CVersion v) {
        VersionDetailDTO dto = new VersionDetailDTO();
        dto.setVersionId(v.getVersionId());
        dto.setHash(v.getHash());
        dto.setCommitMessage(v.getCommitMessage());
        dto.setSerializedTree(v.getSerializedTree());
        dto.setCommitTimestamp(v.getCommitTimestamp());
        dto.setReviewStatus(v.getReviewStatus());
        dto.setReviewerNote(v.getReviewerNote());
        dto.setReviewedAt(v.getReviewedAt());

        if (v.getCivilization() != null) {
            dto.setCivId(v.getCivilization().getCivId());
            dto.setCivTitle(v.getCivilization().getTitle());
            dto.setCivDescription(v.getCivilization().getDescription());
            dto.setCivStartDate(v.getCivilization().getStartDate());
            dto.setCivEndDate(v.getCivilization().getEndDate());
            if (v.getCivilization().getUniversity() != null) {
                dto.setUniversityId(v.getCivilization().getUniversity().getId());
                dto.setUniversityName(v.getCivilization().getUniversity().getName());
            }
        }

        if (v.getCommittedBy() != null) {
            dto.setCommittedById(v.getCommittedBy().getUserId());
            dto.setCommittedByName(v.getCommittedBy().getFirstName()
                    + " " + v.getCommittedBy().getLastName());
            dto.setCommittedByEmail(v.getCommittedBy().getEmail());
        }

        if (v.getReviewedBy() != null)
            dto.setReviewedByName(v.getReviewedBy().getFirstName()
                    + " " + v.getReviewedBy().getLastName());

        return dto;
    }
// ── Inject in constructor ─────────────────────────────────────────────────
// private final VolumeReviewMarkRepository volumeReviewMarkRepository;

    @Transactional
    public UUID markVolume(MarkVolumeDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CVersion version = cVersionRepository.findById(dto.getVersionId())
                .orElseThrow(() -> new RuntimeException("Version not found"));

        VolumeReviewMark mark = volumeReviewMarkRepository
                .findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
                        reviewer.getReviewerId(), dto.getVersionId(), dto.getNodeId())
                .orElse(new VolumeReviewMark());

        mark.setReviewer(reviewer);
        mark.setVersion(version);
        mark.setNodeId(dto.getNodeId());
        mark.setVolumeTitle(dto.getVolumeTitle());
        mark.setStartYear(dto.getStartYear());
        mark.setEndYear(dto.getEndYear());
        mark.setMarkStatus(VolumeMarkStatus.SAVED);
        mark.setReviewerNote(dto.getReviewerNote());

        VolumeReviewMark saved = volumeReviewMarkRepository.save(mark);
        return saved.getMarkId();
    }

    public List<VolumeMarkResponseDTO> getMyVolumeMarks() {
        Reviewer reviewer = getAuthenticatedReviewer();
        return volumeReviewMarkRepository
                .findByReviewer_ReviewerId(reviewer.getReviewerId())
                .stream().map(this::toVolumeMarkDTO).toList();
    }

    @Transactional
    public void deleteVolumeMark(UUID markId) {
        Reviewer reviewer = getAuthenticatedReviewer();
        if (!volumeReviewMarkRepository.existsByMarkIdAndReviewer_ReviewerId(
                markId, reviewer.getReviewerId()))
            throw new RuntimeException("Volume mark not found or does not belong to you");
        volumeReviewMarkRepository.deleteByMarkIdAndReviewer_ReviewerId(
                markId, reviewer.getReviewerId());
    }

    private VolumeMarkResponseDTO toVolumeMarkDTO(VolumeReviewMark m) {
        VolumeMarkResponseDTO dto = new VolumeMarkResponseDTO();
        dto.setMarkId(m.getMarkId());
        dto.setVersionId(m.getVersion().getVersionId());
        dto.setNodeId(m.getNodeId());
        dto.setVolumeTitle(m.getVolumeTitle());
        dto.setStartYear(m.getStartYear());
        dto.setEndYear(m.getEndYear());
        dto.setMarkStatus(m.getMarkStatus());
        dto.setReviewerNote(m.getReviewerNote());
        dto.setMarkedAt(m.getMarkedAt());

        if (m.getVersion().getCivilization() != null) {
            var civ = m.getVersion().getCivilization();
            dto.setCivTitle(civ.getTitle());
            if (civ.getUniversity() != null)
                dto.setUniversityName(civ.getUniversity().getName());
        }
        if (m.getVersion().getCommittedBy() != null) {
            var u = m.getVersion().getCommittedBy();
            dto.setCommittedByName(u.getFirstName() + " " + u.getLastName());
        }
        return dto;
    }
    // ── Helper ────────────────────────────────────────────────────────────────

    private Reviewer getAuthenticatedReviewer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal().toString();
        return reviewerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
    }
}