package com.mgwprod.challenges.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SubmissionNotFoundException extends ApiException {
    public SubmissionNotFoundException(Long submissionId) {
        super(HttpStatus.NOT_FOUND, "No existe una submission con id: " + submissionId);
    }
}
