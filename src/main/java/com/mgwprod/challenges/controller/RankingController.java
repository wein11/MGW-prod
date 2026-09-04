package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.RankingEntry;
import com.mgwprod.challenges.service.ChallengeResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RankingController {

    private final ChallengeResultService challengeResultService;

    public RankingController(ChallengeResultService challengeResultService) {
        this.challengeResultService = challengeResultService;
    }

    @GetMapping("/api/ranking")
    public List<RankingEntry> ranking() {
        return challengeResultService.ranking();
    }

    @GetMapping("/api/challenges/results")
    public List<ChallengeResult> results(@RequestParam(required = false) Long producerId) {
        return challengeResultService.listResults(producerId);
    }
}
