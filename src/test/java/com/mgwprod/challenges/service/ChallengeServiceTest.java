package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private ChallengeResultRepository challengeResultRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void canCreateChallengeIsTrueForAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThat(challengeService.canCreateChallenge(1L)).isTrue();
    }

    @Test
    void canCreateChallengeIsTrueForDiscografica() {
        User discografica = new User();
        discografica.setId(1L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(1L)).thenReturn(Optional.of(discografica));

        assertThat(challengeService.canCreateChallenge(1L)).isTrue();
    }

    @Test
    void canCreateChallengeIsFalseForArtist() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThat(challengeService.canCreateChallenge(1L)).isFalse();
    }

    @Test
    void isArtistIsFalseWhenGuestIsNotAnArtist() {
        User notArtist = new User();
        notArtist.setId(2L);
        notArtist.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notArtist));

        assertThat(challengeService.isArtist(2L)).isFalse();
    }

    @Test
    void createSetsCreatedByAndSaves() {
        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge created = challengeService.create(1L, challenge);

        assertThat(created.getCreatedBy()).isEqualTo(1L);
        assertThat(created.getGuestArtistId()).isEqualTo(2L);
    }

    @Test
    void setOpportunityPickSucceedsWhenSubmissionBelongsToTheChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);

        Submission submission = new Submission();
        submission.setId(7L);
        submission.setChallenge(challenge);
        when(submissionService.getById(7L)).thenReturn(submission);

        when(challengeRepository.save(challenge)).thenReturn(challenge);

        Challenge result = challengeService.setOpportunityPick(challenge, 7L);

        assertThat(result.getOpportunityPickSubmissionId()).isEqualTo(7L);
    }

    @Test
    void setOpportunityPickReturnsNullWhenSubmissionBelongsToAnotherChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        Challenge otherChallenge = new Challenge();
        otherChallenge.setId(200L);

        Submission submission = new Submission();
        submission.setId(7L);
        submission.setChallenge(otherChallenge);
        when(submissionService.getById(7L)).thenReturn(submission);

        assertThat(challengeService.setOpportunityPick(challenge, 7L)).isNull();
    }

    @Test
    void canModifyIsTrueForCreator() {
        Challenge challenge = new Challenge();
        challenge.setCreatedBy(1L);

        assertThat(challengeService.canModify(challenge, 1L)).isTrue();
    }

    @Test
    void canModifyIsTrueForAdmin() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        Challenge challenge = new Challenge();
        challenge.setCreatedBy(1L);

        assertThat(challengeService.canModify(challenge, 9L)).isTrue();
    }

    @Test
    void updateChangesFields() {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setTitle("Viejo");
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(challenge)).thenReturn(challenge);

        Challenge request = new Challenge();
        request.setTitle("Nuevo");

        Challenge updated = challengeService.update(100L, request);

        assertThat(updated.getTitle()).isEqualTo("Nuevo");
    }

    @Test
    void isClosedReflectsChallengeResultRepository() {
        when(challengeResultRepository.existsByChallengeId(100L)).thenReturn(true);

        assertThat(challengeService.isClosed(100L)).isTrue();
    }

    @Test
    void deleteRemovesChallenge() {
        challengeService.delete(100L);

        verify(challengeRepository).deleteById(100L);
    }
}
