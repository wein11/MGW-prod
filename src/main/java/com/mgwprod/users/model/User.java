package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // No se guarda en la base: solo trae la contraseña del registro para hashearla.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Se ejecuta antes del INSERT.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
