package com.example.nexusa.Repository.Central;

import com.example.nexusa.Model.Central.CentralCivilization;
import com.example.nexusa.Model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CentralCivilizationRepository extends JpaRepository<CentralCivilization, UUID> {

    boolean existsByTitle(String title);
    List<CentralCivilization> findAll();
    /**
     * Retrieves all civilization matrices authored by a specific user.
     * Adapt the parameter type (String vs UUID) to match your entity property.
     */
    List<CentralCivilization> findByCreatedBy(Reviewer createdBy);
    List<CentralCivilization> findByTitleContainingIgnoreCase(String titleFragment);
    
    // Alternative option if your entity tracks a raw UUID owner id:
    // List<CentralCivilization> findByOwnerId(UUID ownerId);
}