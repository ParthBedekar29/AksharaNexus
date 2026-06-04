// com/example/nexusa/Model/ReviewerCode.java
package com.example.nexusa.Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviewer_codes")
@Data
public class ReviewerCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "code_id")
    private UUID codeId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}