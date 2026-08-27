package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "Ya existe un usuario con el email: " + email);
    }
}
