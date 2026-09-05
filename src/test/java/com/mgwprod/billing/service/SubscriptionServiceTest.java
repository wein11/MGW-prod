package com.mgwprod.billing.service;

import com.mgwprod.billing.exception.SubscriptionLimitExceededException;
import com.mgwprod.billing.gateway.PaymentGateway;
import com.mgwprod.billing.gateway.PaymentResult;
import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import com.mgwprod.billing.repository.SubscriptionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getOrCreateCreatesFreeSubscriptionWhenMissing() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription subscription = subscriptionService.getOrCreate(1L);

        assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(subscription.getUserId()).isEqualTo(1L);
    }

    @Test
    void getOrCreateThrowsForNonArtistRoles() {
        User label = new User();
        label.setId(2L);
        label.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(label));

        assertThatThrownBy(() -> subscriptionService.getOrCreate(2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void upgradeChargesAndSetsPremium() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "SIMULATED-abc"));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription upgraded = subscriptionService.upgrade(1L);

        assertThat(upgraded.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
    }

    @Test
    void downgradeSetsFree() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription downgraded = subscriptionService.downgrade(1L);

        assertThat(downgraded.getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    void recordProductionIncrementsCountWhenUnderLimit() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setProductionsCount(10);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.recordProduction(1L);

        assertThat(subscription.getProductionsCount()).isEqualTo(11);
    }

    @Test
    void recordProductionThrowsWhenFreeLimitReached() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setProductionsCount(50);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.recordProduction(1L))
                .isInstanceOf(SubscriptionLimitExceededException.class);
    }

    @Test
    void recordProductionNeverThrowsForPremium() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.PREMIUM);
        subscription.setProductionsCount(500);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.recordProduction(1L);

        assertThat(subscription.getProductionsCount()).isEqualTo(501);
    }
}
