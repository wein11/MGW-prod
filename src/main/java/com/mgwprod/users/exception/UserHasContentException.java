package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserHasContentException extends ApiException {
    public UserHasContentException(Long userId) {
        super(HttpStatus.CONFLICT, "No se puede borrar el usuario " + userId + ": tiene contenido asociado (beats, toplines, challenges, comentarios, votos, etc.)");
    }
}
