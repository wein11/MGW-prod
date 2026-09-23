package com.mgwprod.billing.gateway;

import java.math.BigDecimal;

// Interfaz para poder cambiar el pago simulado por uno real sin tocar el service.
public interface PaymentGateway {
    PaymentResult charge(Long userId, BigDecimal amount);
}
