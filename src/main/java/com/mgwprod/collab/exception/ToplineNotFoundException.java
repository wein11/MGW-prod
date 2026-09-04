package com.mgwprod.collab.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ToplineNotFoundException extends ApiException {
    public ToplineNotFoundException(Long toplineId) {
        super(HttpStatus.NOT_FOUND, "No existe un topline con id: " + toplineId);
    }
}
