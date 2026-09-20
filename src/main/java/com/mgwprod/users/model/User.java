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

// Entidad JPA mapeada directo a la tabla "users" (sin DTOs: el controller devuelve
// esta clase tal cual como JSON). Representa cualquier cuenta registrada: artistas,
// sellos (DISCOGRAFICA) y admins usan la misma tabla, distinguidos por `role`.
@Entity
@Table(name = "users")
@Getter
@Setter
// Sin @AllArgsConstructor: nada en el código lo llama, y con la compilación
// -parameters, Jackson 3 lo toma como constructor implícito para deserializar, lo cual
// rompe si el JSON no manda alguna clave (ej. sin "isAdmin"), porque intenta pasarle
// null a un parámetro boolean primitivo. Dejando solo el constructor vacío, Jackson
// deserializa seteando propiedad por propiedad (con los setters), y tolera que falten claves.
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

    // No se persiste: solo transporta la contraseña en texto plano desde
    // POST /api/auth/register hacia AuthService, que la hashea en passwordHash.
    // Es write-only: se acepta en el JSON de entrada pero nunca se devuelve en una respuesta.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // Se guarda el nombre del enum ("ARTIST"/"DISCOGRAFICA"/"ADMIN") en vez de un
    // número ordinal, para que la columna sea legible directamente en la base y no se
    // rompa si en algún momento se reordenan los valores del enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Callback de ciclo de vida de JPA: se ejecuta automáticamente justo antes de que
    // Hibernate haga el INSERT de esta entidad, así todo usuario queda con una fecha
    // de creación puesta por el servidor sin que quien llama tenga que enviarla.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
