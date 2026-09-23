package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Tabla propia de sesiones de login: un token aleatorio que se le entrega al cliente
// al loguearse, y que SessionAuthInterceptor busca en cada request para resolver qué
// User está llamando.
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un mismo usuario puede tener varias sesiones a la vez (ej. logueado desde dos dispositivos).
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // String aleatorio y opaco que se devuelve al cliente al loguearse; el cliente lo
    // reenvía (ej. en un header) en cada request siguiente para demostrar quién es.
    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
