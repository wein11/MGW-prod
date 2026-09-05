package com.mgwprod.billing.gateway;

import java.math.BigDecimal;

// El día de mañana, una MercadoPagoGateway (o cualquier otra pasarela real) implementa
// esta misma interfaz y se enchufa sin tocar SubscriptionService ni ningún otro
// consumidor — el service depende de la interfaz, nunca de la implementación concreta.
public interface PaymentGateway {
    PaymentResult charge(Long userId, BigDecimal amount);
}
