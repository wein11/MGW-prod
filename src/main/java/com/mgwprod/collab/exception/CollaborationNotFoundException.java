package com.mgwprod.collab.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CollaborationNotFoundException extends ApiException {
    public CollaborationNotFoundException(Long collaborationId) {
        super(HttpStatus.NOT_FOUND, "No existe una colaboración con id: " + collaborationId);
    }
}
