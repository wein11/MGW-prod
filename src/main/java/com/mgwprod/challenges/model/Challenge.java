package com.mgwprod.challenges.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

// Un concurso semanal: define género/BPM/tema, tiene un artista invitado como jurado y
// una fecha límite. Los productores mandan Submission hasta el deadline, después se
// cierra y se calculan los resultados (ver ChallengeResultService).
@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Server-derived desde el requester autenticado (ChallengeService.create) — nunca
    // viaja en el JSON del cliente.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private Integer bpm;

    @Column(name = "music_key")
    private String key;

    private String theme;

    @Column(nullable = false)
    private Instant deadline;

    // El artista invitado actúa como jurado: es el único que puede elegir el
    // "opportunity pick" entre las submissions (ver ChallengeService.isGuestArtist).
    @Column(name = "guest_artist_id", nullable = false)
    private Long guestArtistId;

    @Column(name = "prize_first")
    private String prizeFirst;

    @Column(name = "prize_second")
    private String prizeSecond;

    @Column(name = "prize_third")
    private String prizeThird;

    // Submission elegida a mano por el artista invitado como su favorita, aparte del
    // ranking por votos/puntaje. Queda en null hasta que se elige una.
    @Column(name = "opportunity_pick_submission_id")
    private Long opportunityPickSubmissionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Lado inverso del @ManyToOne de Submission — se llena solo con un SELECT, nunca
    // se persiste desde acá. @JsonIgnore evita el ciclo Challenge -> submissions ->
    // Submission -> challenge -> ...
    @OneToMany(mappedBy = "challenge")
    @JsonIgnore
    private List<Submission> submissions;

    // Se ejecuta solo antes del INSERT y guarda la fecha de creación.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
