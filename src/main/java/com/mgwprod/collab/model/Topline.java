package com.mgwprod.collab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgwprod.catalog.model.Beat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Una melodía/voz (topline) que un artista graba sobre un Beat existente, para
// proponerle una colaboración al productor dueño del beat.
@Entity
@Table(name = "toplines")
@Getter
@Setter
@NoArgsConstructor
public class Topline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Server-derived desde la sesión autenticada (ToplineService.create) — nunca
    // viaja en el JSON del cliente.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    // Relación hacia el módulo catalog: un topline siempre pertenece a un Beat
    // concreto. Al ser un objeto (no un id suelto), Spring Data puede derivar
    // findByBeatId directamente sobre esta relación.
    @ManyToOne
    @JoinColumn(name = "beat_id", nullable = false)
    private Beat beat;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Sella la fecha de creación automáticamente antes del INSERT.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
