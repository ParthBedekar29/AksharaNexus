package com.example.nexusa.Model;

import com.example.nexusa.Model.Enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "public_users")
@Data
public class PublicUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    @Column(nullable = false)
    private Role role = Role.VIEWER;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false ,columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "publicUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatSession> chatSessions = new ArrayList<>();

    @OneToMany(mappedBy = "publicUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PublicUserEmailVerificationToken> verificationTokens = new ArrayList<>();

    @OneToMany(mappedBy = "publicUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PublicUserPasswordResetToken> passwordResetTokens = new ArrayList<>();


}