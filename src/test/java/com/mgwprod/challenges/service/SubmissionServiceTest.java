package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void createSavesSubmissionWhenProducerAndDeadlineAreValid() {
        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDeadline(Instant.now().plusSeconds(3600));
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission saved = submissionService.create(1L, 2L, submission);

        assertThat(saved.getChallengeId()).isEqualTo(1L);
        assertThat(saved.getProducerId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenDeadlineHasPassed() {
        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDeadline(Instant.now().minusSeconds(3600));
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");

        assertThatThrownBy(() -> submissionService.create(1L, 2L, submission))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createThrowsWhenUserIsNotProducer() {
        User artist = new User();
        artist.setId(2L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");

        assertThatThrownBy(() -> submissionService.create(1L, 2L, submission))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
