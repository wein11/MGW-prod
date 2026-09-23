package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.service.SubmissionService;
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

// Endpoints anidados bajo /api/challenges/{challengeId}/submissions.
@RestController
@RequestMapping("/api/challenges/{challengeId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    // Orden: 401 -> 400 (campos) -> 403 (rol) -> 404 (challenge) -> 403 (deadline
    // vencido). Se chequea el rol antes de buscar el challenge para no gastar una
    // consulta a la base si total el usuario ni siquiera puede participar.
    @PostMapping
    public ResponseEntity<Submission> createSubmission(@PathVariable Long challengeId,
                                                         @RequestAttribute(name = "userId", required = false) Long userId,
                                                         @RequestBody Submission submission) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (submission.getAudioUrl() == null || submission.getAudioUrl().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!submissionService.isArtist(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Challenge challenge = submissionService.getChallenge(challengeId);
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (submissionService.isPastDeadline(challenge)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Submission created = submissionService.create(challengeId, userId, submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/challenges/{challengeId}/submissions -> lista las entregas del challenge.
    @GetMapping
    public List<Submission> listSubmissions(@PathVariable Long challengeId) {
        return submissionService.listByChallenge(challengeId);
    }
}
