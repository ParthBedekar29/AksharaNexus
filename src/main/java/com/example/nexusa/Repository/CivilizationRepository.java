package com.example.nexusa.Repository;

import com.example.nexusa.Model.Civilization;
import com.example.nexusa.Model.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CivilizationRepository extends JpaRepository<Civilization, UUID> {
    List<Civilization> findByUniversity_Id(UUID uniId);

    // In CivilizationRepository.java
    List<Civilization> findByUniversity(University university);
}