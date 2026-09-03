package com.mgwprod.collab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // Server-derived from the authenticated session (ToplineService.create), same
    // pattern as User.passwordHash: never accepted from client JSON, so no @NotNull
    // here — @Valid at the controller runs before the service sets this field.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @NotNull(message = "El beat es obligatorio")
    @Column(name = "beat_id", nullable = false)
    private Long beatId;

    @NotBlank(message = "El link de audio es obligatorio")
    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
