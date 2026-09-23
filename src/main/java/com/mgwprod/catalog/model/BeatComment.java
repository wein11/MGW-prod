package com.mgwprod.catalog.model;

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

// Un comentario que alguien deja sobre un Beat.
@Entity
@Table(name = "beat_comments")
@Getter
@Setter
@NoArgsConstructor
public class BeatComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación real hacia Beat en vez de guardar solo un beatId suelto: al ser un
    // objeto, Hibernate puede resolver solo el JOIN cuando se necesite el beat completo,
    // y Spring Data puede derivar queries como findByBeatId a partir de esta relación.
    // Lo setea el service a partir del path (beatId) — nunca viaja en el JSON del cliente.
    @ManyToOne
    @JoinColumn(name = "beat_id", nullable = false)
    private Beat beat;

    // Lo setea el service a partir del userId autenticado.
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Se ejecuta solo antes del INSERT y guarda la fecha de creación.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
