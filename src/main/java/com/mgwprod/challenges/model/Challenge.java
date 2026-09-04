package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "El género es obligatorio")
    @Column(nullable = false)
    private String genre;

    @NotNull(message = "El BPM es obligatorio")
    @Min(value = 1, message = "El BPM debe ser mayor a 0")
    @Column(nullable = false)
    private Integer bpm;

    @Column(name = "music_key")
    private String key;

    private String theme;

    @NotNull(message = "El deadline es obligatorio")
    @Column(nullable = false)
    private Instant deadline;

    @NotNull(message = "El artista invitado es obligatorio")
    @Column(name = "guest_artist_id", nullable = false)
    private Long guestArtistId;

    @Column(name = "prize_first")
    private String prizeFirst;

    @Column(name = "prize_second")
    private String prizeSecond;

    @Column(name = "prize_third")
    private String prizeThird;

    @Column(name = "opportunity_pick_submission_id")
    private Long opportunityPickSubmissionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
