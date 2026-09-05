package com.mgwprod.catalog.service;

import com.mgwprod.billing.exception.SubscriptionLimitExceededException;
import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeatServiceTest {

    @Mock
    private BeatRepository beatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private BeatService beatService;

    @Test
    void createSavesBeatWhenArtistIsValid() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");
        beat.setGenre("Trap");
        beat.setBpm(140);
        beat.setAudioUrl("https://soundcloud.com/example/trap-beat");

        when(beatRepository.save(any(Beat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Beat saved = beatService.create(1L, beat);

        assertThat(saved.getProducerId()).isEqualTo(1L);
    }

    @Test
    void createThrowsWhenUserIsDiscografica() {
        User discografica = new User();
        discografica.setId(2L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(discografica));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");

        assertThatThrownBy(() -> beatService.create(2L, beat))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createCallsRecordProductionBeforeSaving() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");
        beat.setGenre("Trap");
        beat.setBpm(140);
        beat.setAudioUrl("https://soundcloud.com/example/trap-beat");
        when(beatRepository.save(any(Beat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        beatService.create(1L, beat);

        verify(subscriptionService).recordProduction(1L);
    }

    @Test
    void createPropagatesSubscriptionLimitExceeded() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");

        doThrow(new SubscriptionLimitExceededException())
                .when(subscriptionService).recordProduction(1L);

        assertThatThrownBy(() -> beatService.create(1L, beat))
                .isInstanceOf(SubscriptionLimitExceededException.class);
        verify(beatRepository, never()).save(any());
    }
}
