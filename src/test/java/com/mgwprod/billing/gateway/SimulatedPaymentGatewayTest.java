package com.mgwprod.billing.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    @Test
    void chargeAlwaysApproves() {
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

        PaymentResult result = gateway.charge(1L, new BigDecimal("15.00"));

        assertThat(result.approved()).isTrue();
        assertThat(result.reference()).isNotBlank();
    }
}
