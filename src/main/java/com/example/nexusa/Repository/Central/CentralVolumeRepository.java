package com.example.nexusa.Repository.Central;

import com.example.nexusa.Model.Central.CentralVolume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CentralVolumeRepository extends JpaRepository<CentralVolume, UUID> {

    // Ordered by position for consistent tree rendering
    List<CentralVolume> findByCivilization_CentralCivIdOrderByPosition(UUID centralCivId);

    // Check position slot is free before inserting
    boolean existsByCivilization_CentralCivIdAndPosition(UUID centralCivId, Integer position);
}