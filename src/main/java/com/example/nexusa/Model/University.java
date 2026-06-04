package com.example.nexusa.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;


import java.util.List;
import java.util.UUID;



@Entity
@Table(name = "universities")
@Data
public class University {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uni_id", nullable = false)
    private UUID id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "university")
    @JsonIgnore
    private List<UniversityDomain> domains;

    @Column(name="ror_id")
    private String rorId;

}