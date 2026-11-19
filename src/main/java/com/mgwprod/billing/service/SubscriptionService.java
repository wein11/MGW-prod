package com.mgwprod.billing.service;

import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SubscriptionService {

    private static final BigDecimal PREMIUM_PRICE_USD = new BigDecimal("15.00");
    private static final int FREE_PLAN_LIMIT = 50;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                PaymentGateway paymentGateway,
                                UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userRepository.findById(userId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // Si el usuario no tiene suscripción, se le crea una FREE.
    @Transactional
    public Subscription getOrCreate(Long userId) {
        Subscription existing = subscriptionRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(SubscriptionPlan.FREE);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription upgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        if (subscription.getPlan() == SubscriptionPlan.PREMIUM) {
            return subscription;
        }
        PaymentResult result = paymentGateway.charge(userId, PREMIUM_PRICE_USD);
        if (result.approved()) {
            subscription.setPlan(SubscriptionPlan.PREMIUM);
        }
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription downgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
        subscription.setPlan(SubscriptionPlan.FREE);
        return subscriptionRepository.save(subscription);
    }

    // No es readOnly porque getOrCreate puede hacer un INSERT.
    @Transactional
    public boolean isAtProductionLimit(Long userId) {
        Subscription subscription = getOrCreate(userId);
        return subscription.getPlan() == SubscriptionPlan.FREE && subscription.getProductionsCount() >= FREE_PLAN_LIMIT;
    }

    @Transactional
    public void recordProduction(Long userId) {
        Subscription subscription = getOrCreate(userId);
        subscription.setProductionsCount(subscription.getProductionsCount() + 1);
        subscriptionRepository.save(subscription);
    }
}
