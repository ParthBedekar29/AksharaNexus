package com.example.nexusa.Model;

import com.example.nexusa.Model.University;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "university_domains")
@Data
public class UniversityDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String domain;

    @ManyToOne
    @JoinColumn(name = "uni_id")
    @JsonIgnore
    private University university;
}