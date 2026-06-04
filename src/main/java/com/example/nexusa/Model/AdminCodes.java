package com.example.nexusa.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="admin_codes")
@Data
public class AdminCodes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="code_id")
    private UUID codeId;

    @Column(name="code",nullable = false)
    private String code;

    @Column(name="email",nullable = false,unique = true)
    private String email;

    @Column(name="used")
    private boolean used;

    @Column(name="created_at")
    private LocalDateTime timestamp;


}
