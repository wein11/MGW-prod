package com.mgwprod.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "beats")
@Getter
@Setter
@NoArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producer_id", nullable = false)
    private Long producerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private Integer bpm;

    @Column(name = "music_key")
    private String key;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // @JsonIgnore evita el bucle infinito Beat -> comments -> beat -> ...
    @OneToMany(mappedBy = "beat")
    @JsonIgnore
    private List<BeatComment> comments;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
