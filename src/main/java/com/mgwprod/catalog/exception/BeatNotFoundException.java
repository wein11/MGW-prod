package com.mgwprod.catalog.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BeatNotFoundException extends ApiException {
    public BeatNotFoundException(Long beatId) {
        super(HttpStatus.NOT_FOUND, "No existe un beat con id: " + beatId);
    }
}
