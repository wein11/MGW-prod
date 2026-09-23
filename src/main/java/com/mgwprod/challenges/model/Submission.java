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

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "producer_id", nullable = false)
    private Long producerId;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }
}
