package com.mgwprod.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "beat_comments")
@Getter
@Setter
@NoArgsConstructor
public class BeatComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sin @NotNull: los setea el service (beatId viene del path, authorId del
    // userId autenticado) — mismo criterio que Beat.producerId.
    @Column(name = "beat_id", nullable = false)
    private Long beatId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
