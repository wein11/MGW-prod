package com.mgwprod.billing.gateway;

// Lo que devuelve cualquier PaymentGateway al cobrar: si se aprobó, y una referencia
// del pago para trazabilidad. Un record alcanza porque es solo un dato, sin comportamiento.
public record PaymentResult(boolean approved, String reference) {
}
