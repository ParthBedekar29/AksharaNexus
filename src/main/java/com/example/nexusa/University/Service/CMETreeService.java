package com.example.nexusa.University.Service;

import com.example.nexusa.Model.*;
import com.example.nexusa.Model.Enums.CommitType;
import com.example.nexusa.Repository.CVersionRepository;
import com.example.nexusa.Repository.CivilizationRepository;
import com.example.nexusa.Repository.EditorAssignmentRepository;
import com.example.nexusa.Repository.UserRepository;
import org.pvb.persistenttree.api.Enums.TreeType;
import org.pvb.persistenttree.api.NodeID;
import org.pvb.persistenttree.api.PersistentTree;
import org.pvb.persistenttree.core.TreeFactory;
import org.pvb.persistenttree.serial.TreeSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CMETreeService {

    private final TreeSerializer<String, CMENode> serializer;
    private final CVersionRepository cVersionRepository;
    private final CivilizationRepository civilizationRepository;
    private final UserRepository userRepository;
    private final EditorAssignmentRepository editorAssignmentRepository;

    public CMETreeService(CVersionRepository cVersionRepository,
                          CivilizationRepository civilizationRepository,
                          UserRepository userRepository,
                          EditorAssignmentRepository editorAssignmentRepository) {
        this.serializer = new TreeSerializer<>(CMENode.class);
        this.cVersionRepository = cVersionRepository;
        this.civilizationRepository = civilizationRepository;
        this.userRepository = userRepository;
        this.editorAssignmentRepository = editorAssignmentRepository;
    }

    // ── INITIAL VERSION ────────────────────────────────────────────────────

    public CVersion createInitialVersion(Civilization civilization, String commitMsg, User user) {
        PersistentTree<String, CMENode> initTree = TreeFactory.createTree(null, TreeType.N_ARY);
        String serialized = serializer.serialize(initTree);
        String hash = generateHash(serialized + civilization.getCivId().toString()+LocalDateTime.now());

        CVersion init = new CVersion();
        init.setCivilization(civilization);
        init.setCommitMessage(commitMsg);
        init.setCommittedBy(user);
        init.setSerializedTree(serialized);
        init.setHash(hash);
        init.setCommitType(CommitType.MAJOR);
        init.setCommitTimestamp(LocalDateTime.now());
        init.setStartDate(civilization.getStartDate());
        init.setEndDate(civilization.getEndDate());
        return init;
    }

    // ── ADD VOLUME ─────────────────────────────────────────────────────────

    public CVersion addVolume(UUID civId, UUID parentNodeId, CMENode volumeNode,
                              String commitMsg, User user) {
        CVersion latest = getLatestVersion(civId);
        PersistentTree<String, CMENode> tree = serializer.deserialize(
                latest.getSerializedTree(), TreeType.N_ARY);

        NodeID parentId = new NodeID(parentNodeId);
        tree.addChild(parentId, volumeNode);

        return buildNewVersion(civId, tree, commitMsg, CommitType.MAJOR, user,
                latest.getStartDate(), latest.getEndDate());
    }

    // ── ADD ENTRY ──────────────────────────────────────────────────────────

    public CVersion addEntry(UUID civId, UUID parentNodeId, CMENode entryNode,
                             String commitMsg, User user) {
        CVersion latest = getLatestVersion(civId);
        PersistentTree<String, CMENode> tree = serializer.deserialize(
                latest.getSerializedTree(), TreeType.N_ARY);

        NodeID parentId = new NodeID(parentNodeId);
        tree.addChild(parentId, entryNode);

        return buildNewVersion(civId, tree, commitMsg, CommitType.MINOR, user,
                latest.getStartDate(), latest.getEndDate());
    }

    // ── UPDATE ENTRY ───────────────────────────────────────────────────────

    public CVersion updateEntry(UUID civId, UUID nodeId, CMENode updatedNode,
                                String commitMsg, User user) {
        CVersion latest = getLatestVersion(civId);
        PersistentTree<String, CMENode> tree = serializer.deserialize(
                latest.getSerializedTree(), TreeType.N_ARY);

        tree.update(new NodeID(nodeId), updatedNode);

        return buildNewVersion(civId, tree, commitMsg, CommitType.MINOR, user,
                latest.getStartDate(), latest.getEndDate());
    }

    // ── ROLLBACK ───────────────────────────────────────────────────────────

    public CVersion rollback(UUID civId, String hash, User user) {
        Civilization civ = civilizationRepository.findById(civId)
                .orElseThrow(() -> new RuntimeException("Civilization not found"));

        CVersion target = cVersionRepository.findByCivilization_CivIdAndHash(civId, hash)
                .orElseThrow(() -> new RuntimeException("Version not found for given hash"));

        // Create new version pointing to old tree — history preserved
        String newHash = generateHash(target.getSerializedTree() + civId + LocalDateTime.now());

        CVersion rollbackVersion = new CVersion();
        rollbackVersion.setCivilization(civ);
        rollbackVersion.setSerializedTree(target.getSerializedTree());
        rollbackVersion.setHash(newHash);
        rollbackVersion.setCommitMessage("Rollback to " + hash.substring(0, 8));
        rollbackVersion.setCommittedBy(user);
        rollbackVersion.setCommitType(CommitType.MAJOR);
        rollbackVersion.setCommitTimestamp(LocalDateTime.now());
        rollbackVersion.setStartDate(target.getStartDate());
        rollbackVersion.setEndDate(target.getEndDate());
        return rollbackVersion;
    }

    // ── HELPERS ────────────────────────────────────────────────────────────

    private CVersion getLatestVersion(UUID civId) {
        return cVersionRepository.findTopByCivilization_CivIdOrderByCommitTimestampDesc(civId)
                .orElseThrow(() -> new RuntimeException("No versions found"));
    }

    private CVersion buildNewVersion(UUID civId, PersistentTree<String, CMENode> tree,
                                     String commitMsg, CommitType commitType,
                                     User user, Long startDate, Long endDate) {
        Civilization civ = civilizationRepository.findById(civId)
                .orElseThrow(() -> new RuntimeException("Civilization not found"));

        String serialized = serializer.serialize(tree);
        String hash = generateHash(serialized + civId);

        CVersion version = new CVersion();
        version.setCivilization(civ);
        version.setSerializedTree(serialized);
        version.setHash(hash);
        version.setCommitMessage(commitMsg);
        version.setCommittedBy(user);
        version.setCommitType(commitType);
        version.setCommitTimestamp(LocalDateTime.now());
        version.setStartDate(startDate);
        version.setEndDate(endDate);
        return version;
    }

    public String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}