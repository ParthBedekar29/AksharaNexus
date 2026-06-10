// com/example/nexusa/Model/Reviewer.java
package com.example.nexusa.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviewers")
@Data
public class Reviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;
}