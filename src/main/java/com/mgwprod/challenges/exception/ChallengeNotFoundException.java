package com.mgwprod.challenges.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChallengeNotFoundException extends ApiException {
    public ChallengeNotFoundException(Long challengeId) {
        super(HttpStatus.NOT_FOUND, "No existe un challenge con id: " + challengeId);
    }
}
