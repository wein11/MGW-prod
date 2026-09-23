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

// Representa la propuesta de colaboración que se crea automáticamente cuando un
// artista sube un Topline: el productor dueño del beat la acepta o la rechaza.
@Entity
@Table(name = "collaborations")
@Getter
@Setter
@NoArgsConstructor
public class Collaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: cada topline tiene como máximo una colaboración (relación 1 a 1
    // a nivel de datos, aunque acá se mapee como un id simple y no como @OneToOne).
    @Column(name = "topline_id", nullable = false, unique = true)
    private Long toplineId;

    // Arranca en PENDING al crearse y pasa a ACCEPTED/REJECTED cuando el productor decide.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationStatus status;

    // Queda en null mientras está PENDING; se completa recién cuando se decide.
    @Column(name = "decided_at")
    private Instant decidedAt;
}
