package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
