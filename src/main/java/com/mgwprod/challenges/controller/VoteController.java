package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.service.VoteService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions/{submissionId}/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<Vote> createVote(@PathVariable Long submissionId,
                                            @RequestAttribute(name = "userId", required = false) Long userId,
                                            @Valid @RequestBody Vote vote) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para votar");
        }
        Vote created = voteService.create(submissionId, userId, vote);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
