package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos extra que solo tienen sentido para un User con role ARTIST (géneros musicales,
// bio, rango de BPM preferido...). Se separó en su propia tabla/entidad en vez de
// agregar estas columnas directo en User, porque un usuario DISCOGRAFICA/ADMIN nunca
// las necesitaría.
@Entity
@Table(name = "artist_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonIgnore: sin esto, al serializar un perfil se serializaría también su User
    // completo (incluyendo passwordHash) dentro de cada respuesta. El cliente ya tiene
    // el userId desde la URL (/api/users/{id}/profile), no hace falta repetirlo.
    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String genres;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Column(name = "experience_level")
    private String experienceLevel;

    // Solo pasa a true desde UserService.verifyArtist, llamado desde el endpoint
    // PUT /api/artists/{id}/verify (solo accesible por un admin) — un artista no
    // puede autoverificarse.
    @Column(nullable = false)
    private boolean verified = false;
}
