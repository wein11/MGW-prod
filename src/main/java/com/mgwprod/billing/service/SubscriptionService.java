package com.mgwprod.billing.service;

import com.mgwprod.billing.exception.SubscriptionLimitExceededException;
import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
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

    @Transactional
    public Subscription getOrCreate(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFoundException(userId));
                    if (user.getRole() != Role.ARTIST) {
                        throw new ForbiddenOperationException("Solo los artistas tienen suscripción");
                    }
                    Subscription subscription = new Subscription();
                    subscription.setUserId(userId);
                    subscription.setPlan(SubscriptionPlan.FREE);
                    return subscriptionRepository.save(subscription);
                });
    }

    @Transactional
    public Subscription upgrade(Long userId) {
        Subscription subscription = getOrCreate(userId);
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

    @Transactional
    public void recordProduction(Long userId) {
        Subscription subscription = getOrCreate(userId);
        if (subscription.getPlan() == SubscriptionPlan.FREE && subscription.getProductionsCount() >= FREE_PLAN_LIMIT) {
            throw new SubscriptionLimitExceededException();
        }
        subscription.setProductionsCount(subscription.getProductionsCount() + 1);
        subscriptionRepository.save(subscription);
    }
}
