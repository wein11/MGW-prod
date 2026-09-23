package com.mgwprod.billing.model;

// FREE tiene un límite de producciones (ver SubscriptionService.FREE_PLAN_LIMIT);
// PREMIUM no tiene límite.
public enum SubscriptionPlan {
    FREE,
    PREMIUM
}
