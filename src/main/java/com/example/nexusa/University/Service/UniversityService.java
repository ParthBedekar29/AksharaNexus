package com.example.nexusa.University.Service;

import com.example.nexusa.Repository.UniversityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UniversityService {
    private final UniversityRepository universityRepository;

    public UniversityService(UniversityRepository universityRepository) {
        this.universityRepository = universityRepository;
    }

    public List<Map<String, String>> getAllUniversities() {
        return universityRepository.findAll()
                .stream()
                .map(u -> Map.of("name", u.getName(), "id", u.getId().toString()))
                .toList();
    }
}