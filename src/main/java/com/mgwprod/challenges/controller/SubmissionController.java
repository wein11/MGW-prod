package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.service.SubmissionService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/challenges/{challengeId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<Submission> createSubmission(@PathVariable Long challengeId,
                                                         @RequestAttribute(name = "userId", required = false) Long userId,
                                                         @Valid @RequestBody Submission submission) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para enviar una submission");
        }
        Submission created = submissionService.create(challengeId, userId, submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Submission> listSubmissions(@PathVariable Long challengeId) {
        return submissionService.listByChallenge(challengeId);
    }
}
