package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
    }
}
