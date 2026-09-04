package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChallengeCloseController {

    private final ChallengeResultService challengeResultService;

    public ChallengeCloseController(ChallengeResultService challengeResultService) {
        this.challengeResultService = challengeResultService;
    }

    @PutMapping("/api/challenges/{id}/close")
    public List<ChallengeResult> close(@PathVariable Long id,
                                        @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para cerrar un challenge");
        }
        return challengeResultService.close(id, requestingUserId);
    }
}
