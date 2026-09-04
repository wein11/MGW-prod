package com.mgwprod.catalog.model;

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
@Table(name = "beats")
@Getter
@Setter
@NoArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sin @NotNull: lo asigna el servicio a partir del userId autenticado, nunca
    // viaja en el JSON del cliente — igual que id/createdAt en User.java. Con
    // @NotNull acá, @Valid @RequestBody rechazaría siempre con 400 antes de que
    // el service llegue a setearlo.
    @Column(name = "producer_id", nullable = false)
    private Long producerId;

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
