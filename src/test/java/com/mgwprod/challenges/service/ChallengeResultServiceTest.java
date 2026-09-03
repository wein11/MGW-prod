package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeResultServiceTest {

    @Mock
    private ChallengeResultRepository challengeResultRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProducerProfileRepository producerProfileRepository;

    // Colaborador real (función pura, sin dependencias): el test provee votos reales y
    // espera el orden que produce el cálculo ponderado de verdad, no un mock. El plan
    // omitió cablearlo; con @Spy, @InjectMocks lo inyecta con su comportamiento real.
    @Spy
    private ChallengeScoringService challengeScoringService = new ChallengeScoringService();

    @InjectMocks
    private ChallengeResultService challengeResultService;

    @Test
    void closeCreatesTopThreeResultsOrderedByScore() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);
        when(challengeService.getById(100L)).thenReturn(challenge);

        Submission low = new Submission();
        low.setId(1L);
        low.setProducerId(11L);
        Submission mid = new Submission();
        mid.setId(2L);
        mid.setProducerId(12L);
        Submission high = new Submission();
        high.setId(3L);
        high.setProducerId(13L);
        when(submissionRepository.findByChallengeId(100L)).thenReturn(List.of(low, mid, high));

        when(voteRepository.findBySubmissionId(1L)).thenReturn(List.of(voteFrom(1L, 3)));
        when(voteRepository.findBySubmissionId(2L)).thenReturn(List.of(voteFrom(1L, 6)));
        when(voteRepository.findBySubmissionId(3L)).thenReturn(List.of(voteFrom(1L, 9)));

        when(producerProfileRepository.findByVerifiedTrue()).thenReturn(List.of());

        ProducerProfile winnerProfile = new ProducerProfile();
        when(producerProfileRepository.findByUserId(13L)).thenReturn(Optional.of(winnerProfile));
        when(producerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(challengeResultRepository.save(any(ChallengeResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ChallengeResult> results = challengeResultService.close(100L, 1L);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getSubmissionId()).isEqualTo(3L);
        assertThat(results.get(0).getRank()).isEqualTo(1);
        assertThat(results.get(0).getPointsAwarded()).isEqualTo(500);
        assertThat(results.get(1).getSubmissionId()).isEqualTo(2L);
        assertThat(results.get(1).getPointsAwarded()).isEqualTo(300);
        assertThat(results.get(2).getSubmissionId()).isEqualTo(1L);
        assertThat(results.get(2).getPointsAwarded()).isEqualTo(150);
        assertThat(winnerProfile.isVerified()).isTrue();
    }

    @Test
    void closeThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        assertThatThrownBy(() -> challengeResultService.close(100L, 1L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void listResultsFiltersByProducerId() {
        com.mgwprod.challenges.model.Submission submission = new com.mgwprod.challenges.model.Submission();
        submission.setId(5L);
        submission.setProducerId(11L);
        when(submissionRepository.findByProducerId(11L)).thenReturn(List.of(submission));

        ChallengeResult result = new ChallengeResult();
        result.setSubmissionId(5L);
        when(challengeResultRepository.findBySubmissionIdIn(List.of(5L))).thenReturn(List.of(result));

        List<ChallengeResult> found = challengeResultService.listResults(11L);

        assertThat(found).hasSize(1);
    }

    @Test
    void rankingSumsPointsPerProducerDescending() {
        com.mgwprod.challenges.model.Submission submissionA = new com.mgwprod.challenges.model.Submission();
        submissionA.setId(1L);
        submissionA.setProducerId(11L);
        com.mgwprod.challenges.model.Submission submissionB = new com.mgwprod.challenges.model.Submission();
        submissionB.setId(2L);
        submissionB.setProducerId(12L);

        ChallengeResult resultA1 = new ChallengeResult();
        resultA1.setSubmissionId(1L);
        resultA1.setPointsAwarded(150);
        ChallengeResult resultB1 = new ChallengeResult();
        resultB1.setSubmissionId(2L);
        resultB1.setPointsAwarded(500);

        when(challengeResultRepository.findAll()).thenReturn(List.of(resultA1, resultB1));
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submissionA));
        when(submissionRepository.findById(2L)).thenReturn(Optional.of(submissionB));

        List<com.mgwprod.challenges.model.RankingEntry> ranking = challengeResultService.ranking();

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).producerId()).isEqualTo(12L);
        assertThat(ranking.get(0).totalPoints()).isEqualTo(500);
        assertThat(ranking.get(1).producerId()).isEqualTo(11L);
        assertThat(ranking.get(1).totalPoints()).isEqualTo(150);
    }

    private Vote voteFrom(Long voterId, int score) {
        Vote vote = new Vote();
        vote.setVoterId(voterId);
        vote.setScore(score);
        return vote;
    }
}
