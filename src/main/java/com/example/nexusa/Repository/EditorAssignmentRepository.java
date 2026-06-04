package com.example.nexusa.Repository;

import com.example.nexusa.Model.EditorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EditorAssignmentRepository extends JpaRepository<EditorAssignment, UUID> {
    boolean existsByCivilization_CivIdAndEditor_UserId(UUID civId, UUID userId);

    List<EditorAssignment> findByCivilization_CivId(UUID civilizationCivId);
    List<EditorAssignment> findByEditor_UserId(UUID userId);
    void deleteByCivilization_CivId(UUID civId);
}
