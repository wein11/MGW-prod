package com.mgwprod.collab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Un comentario dejado sobre un Topline (distinto de BeatComment, que es sobre un Beat).
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lo setea el service a partir del path variable + la sesión autenticada (ver
    // CommentService.create) — nunca viaja en el JSON del cliente.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "topline_id", nullable = false)
    private Long toplineId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
