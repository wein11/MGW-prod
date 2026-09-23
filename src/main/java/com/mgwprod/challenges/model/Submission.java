package com.mgwprod.challenges.model;

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

// La entrega que un productor manda a un Challenge antes del deadline.
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación real hacia Challenge en vez de guardar solo un id suelto: permite
    // navegar de la submission al challenge completo (ej. para leer su deadline) y que
    // Spring Data derive queries como findByChallengeId.
    // Lo setea el service a partir del path — nunca viaja en el JSON del cliente.
    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    // Lo setea el service a partir del userId autenticado.
    @Column(name = "producer_id", nullable = false)
    private Long producerId;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    // Se ejecuta solo antes del INSERT y guarda la fecha de entrega.
    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }
}
