package com.mgwprod.billing.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Implementación de PaymentGateway que no cobra nada de verdad: siempre aprueba y
// devuelve una referencia falsa. Sirve para poder desarrollar y probar el flujo de
// upgrade sin necesitar una pasarela de pago real todavía.
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long userId, BigDecimal amount) {
        return new PaymentResult(true, "SIMULATED-" + UUID.randomUUID());
    }
}
