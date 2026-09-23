package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChallengeScoringServiceTest {

    private final ChallengeScoringService scoringService = new ChallengeScoringService();

    private Vote voteFrom(Long voterId, int score) {
        Vote vote = new Vote();
        vote.setVoterId(voterId);
        vote.setScore(score);
        return vote;
    }

    @Test
    void weightsCommunityVerifiedAndGuestCorrectly() {
        // comunidad: voterId 1 y 2, promedio (6+8)/2 = 7.0 -> 0.30 * 7.0 = 2.1
        // verificados: voterId 3, promedio 9.0 -> 0.30 * 9.0 = 2.7
        // invitado: voterId 99, score 10 -> 0.40 * 10 = 4.0
        // total esperado: 2.1 + 2.7 + 4.0 = 8.8
        List<Vote> votes = List.of(
                voteFrom(1L, 6),
                voteFrom(2L, 8),
                voteFrom(3L, 9),
                voteFrom(99L, 10)
        );

        double score = scoringService.computeScore(99L, Set.of(3L), votes);

        assertThat(score).isCloseTo(8.8, within(0.001));
    }

    @Test
    void guestArtistPortionIsZeroWhenGuestDidNotVote() {
        // comunidad: voterId 1, score 10 -> 0.30 * 10 = 3.0
        // verificados: sin votos -> 0.30 * 0 = 0.0
        // invitado: no votó -> 0.40 * 0 = 0.0
        // total esperado: 3.0
        List<Vote> votes = List.of(voteFrom(1L, 10));

        double score = scoringService.computeScore(99L, Set.of(), votes);

        assertThat(score).isCloseTo(3.0, within(0.001));
    }

    @Test
    void returnsZeroWhenThereAreNoVotesAtAll() {
        double score = scoringService.computeScore(99L, Set.of(), List.of());

        assertThat(score).isCloseTo(0.0, within(0.001));
    }
}
