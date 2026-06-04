package com.example.nexusa.Repository;

import com.example.nexusa.Model.University;

import com.example.nexusa.Model.UniversityDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {

}
