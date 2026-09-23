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

@Entity
@Table(name = "toplines")
@Getter
@Setter
@NoArgsConstructor
public class Topline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @ManyToOne
    @JoinColumn(name = "beat_id", nullable = false)
    private Beat beat;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
