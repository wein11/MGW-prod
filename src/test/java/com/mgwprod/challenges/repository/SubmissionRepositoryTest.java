package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Test
    void findByChallengeIdReturnsOnlyThatChallengesSubmissions() {
        Challenge challenge = new Challenge();
        challenge.setTitle("Creamos el próximo hit de RKT");
        challenge.setGenre("RKT");
        challenge.setBpm(100);
        challenge.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        challenge.setGuestArtistId(1L);
        challenge.setCreatedBy(1L);
        Challenge savedChallenge = challengeRepository.save(challenge);

        Submission submission = new Submission();
        submission.setChallenge(savedChallenge);
        submission.setProducerId(2L);
        submission.setAudioUrl("https://soundcloud.com/example/submission");
        submissionRepository.save(submission);

        List<Submission> result = submissionRepository.findByChallengeId(savedChallenge.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProducerId()).isEqualTo(2L);
    }
}
