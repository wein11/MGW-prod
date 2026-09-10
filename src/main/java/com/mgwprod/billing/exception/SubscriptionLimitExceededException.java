package com.mgwprod.billing.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SubscriptionLimitExceededException extends ApiException {
    public SubscriptionLimitExceededException() {
        super(HttpStatus.FORBIDDEN, "Llegaste al límite de 50 producciones del plan free — pasate a premium para subir sin límite");
    }
}
