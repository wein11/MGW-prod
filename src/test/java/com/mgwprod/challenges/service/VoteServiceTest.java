package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private VoteService voteService;

    @Test
    void createSavesVoteWhenSubmissionExistsAndNoDuplicate() {
        Submission submission = new Submission();
        submission.setId(1L);
        when(submissionService.getById(1L)).thenReturn(submission);
        when(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).thenReturn(false);

        Vote vote = new Vote();
        vote.setScore(9);
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vote saved = voteService.create(1L, 2L, vote);

        assertThat(saved.getSubmissionId()).isEqualTo(1L);
        assertThat(saved.getVoterId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenVoterAlreadyVotedThisSubmission() {
        Submission submission = new Submission();
        submission.setId(1L);
        when(submissionService.getById(1L)).thenReturn(submission);
        when(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).thenReturn(true);

        Vote vote = new Vote();
        vote.setScore(9);

        assertThatThrownBy(() -> voteService.create(1L, 2L, vote))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
