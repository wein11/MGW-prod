package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Submission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Test
    void findByChallengeIdReturnsOnlyThatChallengesSubmissions() {
        Submission submission = new Submission();
        submission.setChallengeId(1L);
        submission.setProducerId(2L);
        submission.setAudioUrl("https://soundcloud.com/example/submission");
        submissionRepository.save(submission);

        List<Submission> result = submissionRepository.findByChallengeId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProducerId()).isEqualTo(2L);
    }
}
