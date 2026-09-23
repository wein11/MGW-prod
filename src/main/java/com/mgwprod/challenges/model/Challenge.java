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

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // @JsonIgnore evita el bucle infinito Challenge -> submissions -> challenge -> ...
    @OneToMany(mappedBy = "challenge")
    @JsonIgnore
    private List<Submission> submissions;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
