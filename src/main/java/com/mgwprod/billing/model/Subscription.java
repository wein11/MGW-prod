package com.mgwprod.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// El plan de un usuario y cuántas producciones (beats/toplines) lleva creadas, para
// poder aplicar el límite del plan free.
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: un usuario tiene como mucho una suscripción.
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // Todo usuario arranca en FREE — la fila recién se crea la primera vez que se
    // necesita (ver SubscriptionService.getOrCreate), no al registrarse.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    // Cuenta cuántos beats/toplines lleva creados este usuario — es lo que se compara
    // contra el límite del plan free.
    @Column(name = "productions_count", nullable = false)
    private int productionsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Se ejecuta solo antes del INSERT y guarda la fecha de creación.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
