package com.mgwprod.billing.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long userId, BigDecimal amount) {
        return new PaymentResult(true, "SIMULATED-" + UUID.randomUUID());
    }
}
