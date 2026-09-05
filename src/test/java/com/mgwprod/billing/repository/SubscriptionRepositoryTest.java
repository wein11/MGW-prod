package com.mgwprod.billing.repository;

import com.mgwprod.billing.model.Subscription;
import com.mgwprod.billing.model.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void findByUserIdReturnsTheUsersSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(1L);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscriptionRepository.save(subscription);

        Optional<Subscription> result = subscriptionRepository.findByUserId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }
}
