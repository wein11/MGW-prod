package com.mgwprod.billing.controller;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public Subscription me(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.getOrCreate(userId);
    }

    @PostMapping("/upgrade")
    public Subscription upgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.upgrade(userId);
    }

    @PutMapping("/downgrade")
    public Subscription downgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        requireAuthenticated(userId);
        return subscriptionService.downgrade(userId);
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para gestionar tu suscripción");
        }
    }
}
