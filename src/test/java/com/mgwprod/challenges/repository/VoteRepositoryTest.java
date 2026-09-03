package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Vote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class VoteRepositoryTest {

    @Autowired
    private VoteRepository voteRepository;

    @Test
    void findBySubmissionIdReturnsOnlyThatSubmissionsVotes() {
        Vote vote = new Vote();
        vote.setSubmissionId(1L);
        vote.setVoterId(2L);
        vote.setScore(8);
        voteRepository.save(vote);

        List<Vote> result = voteRepository.findBySubmissionId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(8);
    }

    @Test
    void existsBySubmissionIdAndVoterIdDetectsDuplicateVote() {
        Vote vote = new Vote();
        vote.setSubmissionId(1L);
        vote.setVoterId(2L);
        vote.setScore(8);
        voteRepository.save(vote);

        assertThat(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).isTrue();
        assertThat(voteRepository.existsBySubmissionIdAndVoterId(1L, 999L)).isFalse();
    }
}
