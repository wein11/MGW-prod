package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "No existe un usuario con id: " + userId);
    }
}
