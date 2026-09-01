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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
// No @AllArgsConstructor: nothing in the codebase calls it, and with -parameters
// compilation Jackson 3 picks it up as an implicit "properties" creator, which then
// fails on any JSON payload missing a field (e.g. no "isAdmin" key) because it tries
// to bind null into the primitive boolean constructor param. Keeping only the no-arg
// constructor forces bean-style (setter) deserialization, which tolerates missing keys.
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Not persisted: only carries the raw password from POST /api/auth/register into
    // AuthService, which hashes it into passwordHash. Write-only so it's accepted from
    // the request JSON but never echoed back in a response.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre a mostrar es obligatorio")
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;

    // Field name must match the Lombok boolean getter's implicit property name ("admin",
    // from isAdmin() -> "admin") so both merge into one Jackson property before being
    // renamed to "isAdmin" by @JsonProperty. Otherwise Jackson serializes it twice
    // (once as "admin" from the getter, once as "isAdmin" from the annotation).
    @JsonProperty("isAdmin")
    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
