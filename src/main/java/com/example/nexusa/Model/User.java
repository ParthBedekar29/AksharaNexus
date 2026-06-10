package com.example.nexusa.Model;

import com.example.nexusa.Model.Enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="user_id")
    private UUID userId;

    @Column(name = "email", unique = true,nullable = false)
    private String email;

    @Column(name = "password",nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "first_name",nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "uni_id" ,nullable = false)
    private University uniID;

    @Column(name="created_at")
    private LocalDateTime timestamp;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

}
