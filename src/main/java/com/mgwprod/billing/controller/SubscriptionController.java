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

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public ResponseEntity<Subscription> me(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.getOrCreate(userId));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Subscription> upgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.upgrade(userId));
    }

    @PutMapping("/downgrade")
    public ResponseEntity<Subscription> downgrade(@RequestAttribute(name = "userId", required = false) Long userId) {
        ResponseEntity<Subscription> error = checkAccess(userId);
        if (error != null) {
            return error;
        }
        return ResponseEntity.ok(subscriptionService.downgrade(userId));
    }

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
