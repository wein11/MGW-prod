package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.ChallengeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class ChallengeResultRepositoryTest {

    @Autowired
    private ChallengeResultRepository challengeResultRepository;

    @Test
    void findByChallengeIdReturnsOnlyThatChallengesResults() {
        ChallengeResult result = new ChallengeResult();
        result.setChallengeId(1L);
        result.setSubmissionId(10L);
        result.setRank(1);
        result.setPointsAwarded(500);
        challengeResultRepository.save(result);

        List<ChallengeResult> found = challengeResultRepository.findByChallengeId(1L);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPointsAwarded()).isEqualTo(500);
    }
}
