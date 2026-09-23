package com.mgwprod.collab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Se crea sola al subir un topline; el productor del beat la acepta o la rechaza.
@Entity
@Table(name = "collaborations")
@Getter
@Setter
@NoArgsConstructor
public class Collaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topline_id", nullable = false, unique = true)
    private Long toplineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationStatus status;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
