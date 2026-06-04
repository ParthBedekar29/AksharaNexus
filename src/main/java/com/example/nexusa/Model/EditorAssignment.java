package com.example.nexusa.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="editors")
@Data
public class EditorAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "editor_id")
    private UUID editorId;

    @ManyToOne
    @JoinColumn(name = "civ_id", nullable = false)
    @JsonIgnore
    private Civilization civilization;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User editor;

    @ManyToOne
    @JoinColumn(name = "assigned_by", nullable = false)
    @JsonIgnore
    private User assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

}
