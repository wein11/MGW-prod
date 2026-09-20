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

// Entidad central del módulo catalog: un beat/instrumental que un productor publica
// para que los artistas lo puedan ver y, en el módulo collab, grabar un Topline sobre él.
@Entity
@Table(name = "beats")
@Getter
@Setter
@NoArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lo asigna el servicio a partir del userId autenticado, nunca viaja en el
    // JSON del cliente — igual que id/createdAt.
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

    // Lado inverso del @ManyToOne de BeatComment. mappedBy="beat" le dice a Hibernate
    // que este lado no es dueño de la columna FK — la dueña es beat_comments.beat_id —
    // así que esta lista se llena con un SELECT y nunca se escribe de vuelta en la base.
    // @JsonIgnore evita una recursión infinita: Beat -> comments -> BeatComment -> beat -> ...
    @OneToMany(mappedBy = "beat")
    @JsonIgnore
    private List<BeatComment> comments;

    // Se ejecuta solo, justo antes del INSERT, para sellar la fecha de creación sin
    // que el cliente tenga que mandarla.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
