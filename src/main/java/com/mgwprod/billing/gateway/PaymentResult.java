package com.mgwprod.billing.gateway;

public record PaymentResult(boolean approved, String reference) {
}
