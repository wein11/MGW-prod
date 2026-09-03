package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ChallengeScoringService {

    private static final double COMMUNITY_WEIGHT = 0.30;
    private static final double VERIFIED_WEIGHT = 0.30;
    private static final double GUEST_WEIGHT = 0.40;

    public double computeScore(Long guestArtistId, Set<Long> verifiedProducerIds, List<Vote> votes) {
        List<Integer> communityScores = new ArrayList<>();
        List<Integer> verifiedScores = new ArrayList<>();
        Integer guestScore = null;

        for (Vote vote : votes) {
            if (vote.getVoterId().equals(guestArtistId)) {
                guestScore = vote.getScore();
            } else if (verifiedProducerIds.contains(vote.getVoterId())) {
                verifiedScores.add(vote.getScore());
            } else {
                communityScores.add(vote.getScore());
            }
        }

        double communityAvg = average(communityScores);
        double verifiedAvg = average(verifiedScores);
        double guestComponent = guestScore != null ? guestScore : 0.0;

        return COMMUNITY_WEIGHT * communityAvg + VERIFIED_WEIGHT * verifiedAvg + GUEST_WEIGHT * guestComponent;
    }

    private double average(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
}
