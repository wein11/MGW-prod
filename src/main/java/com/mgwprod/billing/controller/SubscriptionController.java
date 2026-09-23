package com.mgwprod.billing.controller;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints REST de las suscripciones: ver el propio plan, subir a premium, bajar a free.
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // GET /api/subscriptions/me -> devuelve el plan del usuario logueado (si no tenía, se le crea FREE).
    @GetMapping("/me")
    public ResponseEntity<Subscription> me(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.getOrCreate(userId));
    }

    // POST /api/subscriptions/upgrade -> cobra (simulado) y pasa el plan a PREMIUM.
    @PostMapping("/upgrade")
    public ResponseEntity<Subscription> upgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.upgrade(userId));
    }

    // PUT /api/subscriptions/downgrade -> vuelve el plan a FREE.
    @PutMapping("/downgrade")
    public ResponseEntity<Subscription> downgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.downgrade(userId));
    }

    // Los tres endpoints hacen exactamente el mismo chequeo de acceso (login, existe,
    // es artista), así que se centraliza acá en vez de repetirlo tres veces. Devuelve
    // null si está todo bien, o el ResponseEntity de error listo para devolver.
    private ResponseEntity<Subscription> checkAccess(Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (!subscriptionService.userExists(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!subscriptionService.isArtist(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return null;
    }
}
