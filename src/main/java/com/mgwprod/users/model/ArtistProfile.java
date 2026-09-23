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

// Datos extra que solo tienen los usuarios con rol ARTIST.
@Entity
@Table(name = "artist_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonIgnore para no devolver el User (con su passwordHash) dentro del perfil.
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

    @Column(nullable = false)
    private boolean verified = false;
}
