package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.challenges.service.ChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChallengeCloseController {

    private final ChallengeResultService challengeResultService;
    private final ChallengeService challengeService;

    public ChallengeCloseController(ChallengeResultService challengeResultService, ChallengeService challengeService) {
        this.challengeResultService = challengeResultService;
        this.challengeService = challengeService;
    }

    @PutMapping("/api/challenges/{id}/close")
    public ResponseEntity<List<ChallengeResult>> close(@PathVariable Long id,
                                                        @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (!challengeResultService.isAdmin(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Challenge challenge = challengeService.getById(id);
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (challengeService.isClosed(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(challengeResultService.close(challenge));
    }
}
