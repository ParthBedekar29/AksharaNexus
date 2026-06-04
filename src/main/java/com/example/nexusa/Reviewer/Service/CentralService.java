package com.example.nexusa.Reviewer.Service;

import com.example.nexusa.Model.CVersion;
import com.example.nexusa.Model.Central.*;
import com.example.nexusa.Model.Enums.EntryMarkStatus;
import com.example.nexusa.Model.Reviewer;
import com.example.nexusa.Repository.CVersionRepository;
import com.example.nexusa.Repository.Central.*;
import com.example.nexusa.Repository.CivilizationRepository;
import com.example.nexusa.Repository.EntryReviewMarkRepository;
import com.example.nexusa.Reviewer.Dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.example.nexusa.Repository.ReviewerRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CentralService {

    private final CentralCivilizationRepository centralCivRepo;
    private final CentralVolumeRepository centralVolumeRepo;
    private final CentralEntryRepository centralEntryRepo;
    private final CentralEntryDivergenceRepository divergenceRepo;
    private final CVersionRepository cVersionRepository;
    private final ReviewerRepository reviewerRepository;
    private final ObjectMapper objectMapper;
    private final CivilizationRepository civilizationRepository;
    private final EntryReviewMarkRepository entryReviewMarkRepository;
    private final ReviewerService reviewerService;

    public CentralService(CentralCivilizationRepository centralCivRepo,
                          CentralVolumeRepository centralVolumeRepo,
                          CentralEntryRepository centralEntryRepo,
                          CentralEntryDivergenceRepository divergenceRepo,
                          CVersionRepository cVersionRepository,
                          ReviewerRepository reviewerRepository,
                          ObjectMapper objectMapper,
                          CivilizationRepository civilizationRepository,
                          EntryReviewMarkRepository entryReviewMarkRepository, ReviewerService reviewerService) {
        this.centralCivRepo = centralCivRepo;
        this.centralVolumeRepo = centralVolumeRepo;
        this.centralEntryRepo = centralEntryRepo;
        this.divergenceRepo = divergenceRepo;
        this.cVersionRepository = cVersionRepository;
        this.reviewerRepository = reviewerRepository;
        this.objectMapper = objectMapper;
        this.civilizationRepository = civilizationRepository;
        this.entryReviewMarkRepository = entryReviewMarkRepository;
        this.reviewerService = reviewerService;
    }

    // ── Auth helper ───────────────────────────────────────────────────────────

    private Reviewer getAuthenticatedReviewer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal().toString();
        return reviewerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
    }

    // ── Create central civilization ───────────────────────────────────────────
// ── Delete a single entry ─────────────────────────────────────────────────

    @Transactional
    public void deleteEntry(UUID centralCivId, UUID volumeId, UUID entryId) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CentralEntry entry = centralEntryRepo.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry not found"));

        // Ownership checks
        if (!entry.getVolume().getVolumeId().equals(volumeId))
            throw new RuntimeException("Entry does not belong to the specified volume");

        if (!entry.getVolume().getCivilization().getCentralCivId().equals(centralCivId))
            throw new RuntimeException("Volume does not belong to the specified civilization");

        // Clean up divergence records that reference this entry (both sides)
        divergenceRepo.deleteByPrimaryEntry_CentralEntryId(entryId);
        divergenceRepo.deleteByConflictingEntry_CentralEntryId(entryId);

        // If the other side of a divergence pair is now clean, clear its isDivergent flag
        // (only if it has no remaining divergence records on either side)
        List<CentralEntry> potentiallyCleared = centralEntryRepo
                .findByVolume_VolumeIdOrderByPosition(volumeId)
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsDivergent()))
                .toList();

        for (CentralEntry candidate : potentiallyCleared) {
            boolean stillDivergent =
                    divergenceRepo.existsByPrimaryEntry_CentralEntryId(candidate.getCentralEntryId()) ||
                            divergenceRepo.existsByConflictingEntry_CentralEntryId(candidate.getCentralEntryId());
            if (!stillDivergent) {
                candidate.setIsDivergent(false);
                centralEntryRepo.save(candidate);
            }
        }

        centralEntryRepo.delete(entry);

        // Bump the parent civ's lastUpdatedAt
        CentralCivilization civ = centralCivRepo.findById(centralCivId)
                .orElseThrow(() -> new RuntimeException("Central civilization not found"));
        civ.setLastUpdatedAt(LocalDateTime.now());
        centralCivRepo.save(civ);
        reindexVolumePositions(volumeId);
    }

// ── Delete a volume (and all its entries) ────────────────────────────────

    @Transactional
    public void deleteVolume(UUID centralCivId, UUID volumeId) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CentralVolume volume = centralVolumeRepo.findById(volumeId)
                .orElseThrow(() -> new RuntimeException("Volume not found"));

        if (!volume.getCivilization().getCentralCivId().equals(centralCivId))
            throw new RuntimeException("Volume does not belong to the specified civilization");

        // Delete each entry's divergence records first, then the entry itself
        List<CentralEntry> entries = centralEntryRepo
                .findByVolume_VolumeIdOrderByPosition(volumeId);

        for (CentralEntry entry : entries) {
            divergenceRepo.deleteByPrimaryEntry_CentralEntryId(entry.getCentralEntryId());
            divergenceRepo.deleteByConflictingEntry_CentralEntryId(entry.getCentralEntryId());
        }
        centralEntryRepo.deleteAll(entries);
        centralVolumeRepo.delete(volume);

        CentralCivilization civ = centralCivRepo.findById(centralCivId)
                .orElseThrow(() -> new RuntimeException("Central civilization not found"));
        civ.setLastUpdatedAt(LocalDateTime.now());
        centralCivRepo.save(civ);
    }

// ── Batch add entries ─────────────────────────────────────────────────────

    @Transactional
    public BatchAddResultDTO addEntriesBatch(UUID centralCivId, UUID volumeId,
                                             BatchAddCentralEntriesDTO batchDto) {
        BatchAddResultDTO result = new BatchAddResultDTO();
        result.setAddedEntryIds(new ArrayList<>());
        result.setFailures(new ArrayList<>());

        for (AddCentralEntryDTO entryDto : batchDto.getEntries()) {
            try {
                UUID id = addEntry(centralCivId, volumeId, entryDto);
                result.getAddedEntryIds().add(id);
            } catch (RuntimeException e) {
                BatchAddResultDTO.FailedEntry failure = new BatchAddResultDTO.FailedEntry();
                failure.setNodeId(entryDto.getSourceNodeId());
                failure.setReason(e.getMessage());
                result.getFailures().add(failure);
            }
        }

        return result;
    }
    @Transactional
    public UUID createCentralCivilization(CreateCentralCivilizationDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        if (centralCivRepo.existsByTitle(dto.getTitle()))
            throw new RuntimeException("A central civilization titled '" + dto.getTitle() + "' already exists");

        CentralCivilization civ = new CentralCivilization();
        civ.setTitle(dto.getTitle());
        civ.setDescription(dto.getDescription());
        civ.setStartYear(dto.getStartYear());
        civ.setEndYear(dto.getEndYear());
        civ.setCreatedBy(reviewer);

        centralCivRepo.save(civ);
        return civ.getCentralCivId();
    }

    public List<CivMetadataDTO> getCivMetadataByTitle(String title) {
        return civilizationRepository.findAll().stream()
                .filter(c -> c.getTitle().equalsIgnoreCase(title))
                .map(c -> {
                    CivMetadataDTO dto = new CivMetadataDTO();
                    dto.setCivId(c.getCivId());
                    dto.setTitle(c.getTitle());
                    dto.setDescription(c.getDescription());
                    dto.setStartYear(c.getStartDate());
                    dto.setEndYear(c.getEndDate());
                    dto.setUniversityName(c.getUniversity().getName());
                    return dto;
                }).toList();
    }

    // ── Add volume ────────────────────────────────────────────────────────────

    @Transactional
    public UUID addVolume(UUID centralCivId, CreateCentralVolumeDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CentralCivilization civ = centralCivRepo.findById(centralCivId)
                .orElseThrow(() -> new RuntimeException("Central civilization not found"));

        if (centralVolumeRepo.existsByCivilization_CentralCivIdAndPosition(
                centralCivId, dto.getPosition()))
            throw new RuntimeException("Position " + dto.getPosition()
                    + " is already taken in this civilization");

        CentralVolume volume = new CentralVolume();
        volume.setCivilization(civ);
        volume.setTitle(dto.getTitle());
        volume.setStartYear(dto.getStartYear());
        volume.setEndYear(dto.getEndYear());
        volume.setPosition(dto.getPosition());
        volume.setCreatedBy(reviewer);
        centralVolumeRepo.save(volume);

        civ.setLastUpdatedAt(LocalDateTime.now());
        centralCivRepo.save(civ);

        return volume.getVolumeId();
    }

    // ── Add entry from a published university version ─────────────────────────
// ── Get central civilizations for the active reviewer ──────────────────────

    public List<CentralCivilizationSummaryDTO> getCentralCivilizationsForCurrentReviewer() {
        // 1. Fetch the active Reviewer object entity using your existing auth helper
        Reviewer currentReviewer = getAuthenticatedReviewer();

        // 2. Query the DB using the correctly named repository instance field
        List<CentralCivilization> civs = centralCivRepo.findByCreatedBy(currentReviewer);

        // 3. Map the entities back to your existing DTO layout using the correct mapper method name
        return civs.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public UUID addEntry(UUID centralCivId, UUID volumeId, AddCentralEntryDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CentralCivilization civ = centralCivRepo.findById(centralCivId)
                .orElseThrow(() -> new RuntimeException("Central civilization not found"));

        CentralVolume volume = centralVolumeRepo.findById(volumeId)
                .orElseThrow(() -> new RuntimeException("Volume not found"));

        if (!volume.getCivilization().getCentralCivId().equals(centralCivId))
            throw new RuntimeException("Volume does not belong to this civilization");

        CVersion sourceVersion = cVersionRepository.findById(dto.getSourceVersionId())
                .orElseThrow(() -> new RuntimeException("Source version not found"));

        boolean entryApproved = entryReviewMarkRepository
                .findByReviewer_ReviewerIdAndVersion_VersionIdAndNodeId(
                        getAuthenticatedReviewer().getReviewerId(),
                        dto.getSourceVersionId(),
                        dto.getSourceNodeId())
                .map(m -> m.getMarkStatus() == EntryMarkStatus.APPROVED)
                .orElse(false);

        if (!entryApproved)
            throw new RuntimeException("Entry must be marked APPROVED before adding to central");

        if (centralEntryRepo.existsBySourceVersion_VersionIdAndSourceNodeId(
                dto.getSourceVersionId(), dto.getSourceNodeId()))
            throw new RuntimeException("This node is already in the central database");

        JsonNode entryNode = extractNodeFromTree(
                sourceVersion.getSerializedTree(), dto.getSourceNodeId());

        if (entryNode == null)
            throw new RuntimeException("Node " + dto.getSourceNodeId()
                    + " not found in source version tree");

        JsonNode data = entryNode.get("data");
        if (data == null || !"ENTRY".equals(data.path("nodeType").asText()))
            throw new RuntimeException("Node must be of type ENTRY");

        String serializedBlocks;
        try {
            serializedBlocks = objectMapper.writeValueAsString(data.get("blocks"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize blocks: " + e.getMessage());
        }

        CentralEntry entry = new CentralEntry();
        entry.setVolume(volume);
        entry.setSourceVersion(sourceVersion);
        entry.setSourceUniversity(sourceVersion.getCivilization().getUniversity());
        entry.setSourceNodeId(dto.getSourceNodeId());
        entry.setTitle(data.path("title").asText("Untitled"));
        entry.setStartYear(data.path("startYear").isNull() ? null : data.path("startYear").asLong());
        entry.setEndYear(data.path("endYear").isNull() ? null : data.path("endYear").asLong());
        entry.setSerializedBlocks(serializedBlocks);
        entry.setPosition(dto.getPosition());
        entry.setIsDivergent(false);
        entry.setAddedBy(reviewer);
        centralEntryRepo.save(entry);

        civ.setLastUpdatedAt(LocalDateTime.now());
        centralCivRepo.save(civ);

        return entry.getCentralEntryId();
    }

    // ── Flag divergence ───────────────────────────────────────────────────────

    @Transactional
    public void flagDivergence(UUID centralCivId, FlagDivergenceDTO dto) {
        Reviewer reviewer = getAuthenticatedReviewer();

        CentralEntry primary = centralEntryRepo.findById(dto.getPrimaryEntryId())
                .orElseThrow(() -> new RuntimeException("Primary entry not found"));

        CentralEntry conflicting = centralEntryRepo.findById(dto.getConflictingEntryId())
                .orElseThrow(() -> new RuntimeException("Conflicting entry not found"));

        if (!primary.getVolume().getCivilization().getCentralCivId().equals(centralCivId) ||
                !conflicting.getVolume().getCivilization().getCentralCivId().equals(centralCivId))
            throw new RuntimeException("Both entries must belong to the same central civilization");

        if (primary.getSourceUniversity().getId().equals(conflicting.getSourceUniversity().getId()))
            throw new RuntimeException("Divergence must be between entries from different universities");

        if (divergenceRepo.existsByPrimaryEntry_CentralEntryIdAndConflictingEntry_CentralEntryId(
                dto.getPrimaryEntryId(), dto.getConflictingEntryId()))
            throw new RuntimeException("Divergence already flagged between these entries");

        CentralEntryDivergence divergence = new CentralEntryDivergence();
        divergence.setPrimaryEntry(primary);
        divergence.setConflictingEntry(conflicting);
        divergence.setDivergenceNote(dto.getDivergenceNote());
        divergence.setFlaggedBy(reviewer);
        divergenceRepo.save(divergence);

        primary.setIsDivergent(true);
        conflicting.setIsDivergent(true);
        centralEntryRepo.save(primary);
        centralEntryRepo.save(conflicting);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<CentralCivilizationSummaryDTO> getAllCentralCivilizations() {
        return centralCivRepo.findAll().stream().map(this::toSummaryDTO).toList();
    }

    public CentralCivilizationDetailDTO getCentralCivilizationDetail(UUID centralCivId) {
        CentralCivilization civ = centralCivRepo.findById(centralCivId)
                .orElseThrow(() -> new RuntimeException("Central civilization not found"));
        return toDetailDTO(civ);
    }

    // ── Tree node extraction ──────────────────────────────────────────────────

    private JsonNode extractNodeFromTree(String serializedTree, String targetNodeId) {
        try {
            JsonNode root = objectMapper.readTree(serializedTree);
            return findNode(root, targetNodeId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse serialized tree: " + e.getMessage());
        }
    }

    private JsonNode findNode(JsonNode node, String targetNodeId) {
        if (node == null) return null;
        if (targetNodeId.equals(node.path("nodeId").asText())) return node;
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                JsonNode found = findNode(child, targetNodeId);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private CentralCivilizationSummaryDTO toSummaryDTO(CentralCivilization civ) {
        CentralCivilizationSummaryDTO dto = new CentralCivilizationSummaryDTO();
        dto.setCentralCivId(civ.getCentralCivId());
        dto.setTitle(civ.getTitle());
        dto.setDescription(civ.getDescription());
        dto.setStartYear(civ.getStartYear());
        dto.setEndYear(civ.getEndYear());
        dto.setCreatedAt(civ.getCreatedAt());
        dto.setLastUpdatedAt(civ.getLastUpdatedAt());
        if (civ.getCreatedBy() != null)
            dto.setCreatedByName(civ.getCreatedBy().getFirstName() + " " + civ.getCreatedBy().getLastName());

        List<CentralVolume> volumes = centralVolumeRepo
                .findByCivilization_CentralCivIdOrderByPosition(civ.getCentralCivId());
        dto.setVolumeCount(volumes.size());

        int entryCount = volumes.stream()
                .mapToInt(v -> centralEntryRepo
                        .findByVolume_VolumeIdOrderByPosition(v.getVolumeId()).size())
                .sum();
        dto.setEntryCount(entryCount);

        return dto;
    }

    private CentralCivilizationDetailDTO toDetailDTO(CentralCivilization civ) {
        CentralCivilizationDetailDTO dto = new CentralCivilizationDetailDTO();
        dto.setCentralCivId(civ.getCentralCivId());
        dto.setTitle(civ.getTitle());
        dto.setDescription(civ.getDescription());
        dto.setStartYear(civ.getStartYear());
        dto.setEndYear(civ.getEndYear());
        dto.setCreatedAt(civ.getCreatedAt());
        dto.setLastUpdatedAt(civ.getLastUpdatedAt());
        if (civ.getCreatedBy() != null)
            dto.setCreatedByName(civ.getCreatedBy().getFirstName() + " " + civ.getCreatedBy().getLastName());

        List<CentralVolume> volumes = centralVolumeRepo
                .findByCivilization_CentralCivIdOrderByPosition(civ.getCentralCivId());

        List<CentralCivilizationDetailDTO.CentralVolumeDTO> volumeDTOs = new ArrayList<>();
        for (CentralVolume vol : volumes) {
            CentralCivilizationDetailDTO.CentralVolumeDTO vDto = new CentralCivilizationDetailDTO.CentralVolumeDTO();
            vDto.setVolumeId(vol.getVolumeId());
            vDto.setTitle(vol.getTitle());
            vDto.setStartYear(vol.getStartYear());
            vDto.setEndYear(vol.getEndYear());
            vDto.setPosition(vol.getPosition());

            List<CentralEntry> entries = centralEntryRepo
                    .findByVolume_VolumeIdOrderByStartYearAscEndYearAsc(vol.getVolumeId());
            List<CentralCivilizationDetailDTO.CentralEntryDTO> entryDTOs = new ArrayList<>();
            for (CentralEntry entry : entries) {
                CentralCivilizationDetailDTO.CentralEntryDTO eDto = new CentralCivilizationDetailDTO.CentralEntryDTO();
                eDto.setCentralEntryId(entry.getCentralEntryId());
                eDto.setTitle(entry.getTitle());
                eDto.setStartYear(entry.getStartYear());
                eDto.setEndYear(entry.getEndYear());
                eDto.setSerializedBlocks(entry.getSerializedBlocks());
                eDto.setPosition(entry.getPosition());
                eDto.setIsDivergent(entry.getIsDivergent());
                eDto.setSourceNodeId(entry.getSourceNodeId());

                if (entry.getSourceVersion() != null) {
                    eDto.setSourceVersionId(entry.getSourceVersion().getVersionId());
                    eDto.setSourceVersionHash(entry.getSourceVersion().getHash());
                }
                if (entry.getSourceUniversity() != null) {
                    eDto.setSourceUniversityId(entry.getSourceUniversity().getId());
                    eDto.setSourceUniversityName(entry.getSourceUniversity().getName());
                }

                if (Boolean.TRUE.equals(entry.getIsDivergent())) {
                    List<CentralEntryDivergence> divs = divergenceRepo
                            .findByPrimaryEntry_CentralEntryId(entry.getCentralEntryId());
                    List<CentralCivilizationDetailDTO.DivergenceDTO> divDTOs = divs.stream()
                            .map(d -> {
                                CentralCivilizationDetailDTO.DivergenceDTO dDto = new CentralCivilizationDetailDTO.DivergenceDTO();
                                dDto.setDivergenceId(d.getDivergenceId());
                                dDto.setConflictingEntryId(d.getConflictingEntry().getCentralEntryId());
                                dDto.setConflictingEntryTitle(d.getConflictingEntry().getTitle());
                                dDto.setConflictingUniversityName(d.getConflictingEntry().getSourceUniversity().getName());
                                dDto.setDivergenceNote(d.getDivergenceNote());
                                return dDto;
                            }).toList();
                    eDto.setDivergences(divDTOs);
                }

                entryDTOs.add(eDto);
            }
            vDto.setEntries(entryDTOs);
            volumeDTOs.add(vDto);
        }
        dto.setVolumes(volumeDTOs);
        return dto;
    }

    private void reindexVolumePositions(UUID volumeId) {
        List<CentralEntry> remaining = centralEntryRepo
                .findByVolume_VolumeIdOrderByStartYearAscEndYearAsc(volumeId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i + 1);
        }
        centralEntryRepo.saveAll(remaining);
    }
}