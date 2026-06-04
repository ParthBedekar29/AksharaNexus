package com.example.nexusa.Repository.Central;

import com.example.nexusa.Model.Central.CentralEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CentralEntryRepository extends JpaRepository<CentralEntry, UUID> {

    List<CentralEntry> findByVolume_VolumeIdOrderByPosition(UUID volumeId);
    List<CentralEntry> findBySourceVersion_VersionId(UUID versionId);
    List<CentralEntry> findBySourceUniversity_Id(UUID universityId);
    boolean existsBySourceVersion_VersionIdAndSourceNodeId(UUID versionId, String nodeId);
    List<CentralEntry> findByVolume_Civilization_CentralCivId(UUID civId);
    List<CentralEntry> findByVolume_VolumeIdAndIsDivergentTrue(UUID volumeId);
    List<CentralEntry> findByVolume_VolumeIdOrderByStartYearAscEndYearAsc(UUID volumeId);
    // Oracle: fuzzy civ name match with optional time range filter
    @Query("""
        SELECT e FROM CentralEntry e
        WHERE LOWER(e.volume.civilization.title) LIKE LOWER(CONCAT('%', :civName, '%'))
        AND (:startYear IS NULL OR e.endYear >= :startYear)
        AND (:endYear IS NULL OR e.startYear <= :endYear)
        ORDER BY e.volume.position, e.position
        """)
    List<CentralEntry> findByCivAndTimeRange(
            @Param("civName") String civName,
            @Param("startYear") Long startYear,
            @Param("endYear") Long endYear
    );

    // Oracle: no time filter, just civ name
    @Query("""
        SELECT e FROM CentralEntry e
        WHERE LOWER(e.volume.civilization.title) LIKE LOWER(CONCAT('%', :civName, '%'))
        ORDER BY e.volume.position, e.position
        """)
    List<CentralEntry> findByCivName(@Param("civName") String civName);
}