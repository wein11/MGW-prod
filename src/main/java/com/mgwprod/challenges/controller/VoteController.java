package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.service.VoteService;
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
                                            @RequestBody Vote vote) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (vote.getScore() == null || vote.getScore() < 1 || vote.getScore() > 10) {
            return ResponseEntity.badRequest().body(null);
        }
        if (voteService.alreadyVoted(submissionId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (voteService.isOwnSubmission(submissionId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Vote created = voteService.create(submissionId, userId, vote);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
