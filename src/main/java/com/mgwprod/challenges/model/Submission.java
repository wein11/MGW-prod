package com.mgwprod.challenges.model;

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
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // challengeId y producerId los setea el SubmissionService desde el path y el userId
    // autenticado, no vienen en el body del cliente. Por eso no llevan @NotNull (rompería
    // el @Valid @RequestBody, que corre antes de que el service los complete); la integridad
    // se garantiza con @Column(nullable = false) + el service que siempre los asigna.
    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "producer_id", nullable = false)
    private Long producerId;

    @NotBlank(message = "El link de audio es obligatorio")
    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }
}
