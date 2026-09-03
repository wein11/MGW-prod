package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.repository.ChallengeRepository;
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
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void createSavesChallengeWhenRequesterIsAdminAndGuestIsArtist() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User guestArtist = new User();
        guestArtist.setId(2L);
        guestArtist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guestArtist));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);
        challenge.setDeadline(Instant.now().plusSeconds(604800));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge saved = challengeService.create(1L, challenge);

        assertThat(saved.getGuestArtistId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);

        assertThatThrownBy(() -> challengeService.create(1L, challenge))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createThrowsWhenGuestArtistIsNotAnArtist() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User notArtist = new User();
        notArtist.setId(2L);
        notArtist.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notArtist));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);

        assertThatThrownBy(() -> challengeService.create(1L, challenge))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
