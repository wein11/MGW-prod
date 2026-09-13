package com.mgwprod.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidFieldException extends ApiException {
    public InvalidFieldException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
