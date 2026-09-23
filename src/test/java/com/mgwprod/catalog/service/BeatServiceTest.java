package com.mgwprod.catalog.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
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
    void createSavesBeat() {
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
    void isArtistReturnsTrueForArtistRole() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThat(beatService.isArtist(1L)).isTrue();
    }

    @Test
    void isArtistReturnsFalseForOtherRoles() {
        User discografica = new User();
        discografica.setId(2L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(discografica));

        assertThat(beatService.isArtist(2L)).isFalse();
    }

    @Test
    void createCallsRecordProductionBeforeSaving() {
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
    void isAtProductionLimitDelegatesToSubscriptionService() {
        when(subscriptionService.isAtProductionLimit(1L)).thenReturn(true);

        assertThat(beatService.isAtProductionLimit(1L)).isTrue();
    }

    @Test
    void updateChangesFields() {
        Beat existing = new Beat();
        existing.setId(1L);
        existing.setProducerId(1L);
        existing.setTitle("Old Title");
        when(beatRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(beatRepository.save(any(Beat.class))).thenAnswer(inv -> inv.getArgument(0));

        Beat request = new Beat();
        request.setTitle("New Title");

        Beat updated = beatService.update(1L, request);

        assertThat(updated.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateReturnsNullWhenBeatMissing() {
        when(beatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(beatService.update(1L, new Beat())).isNull();
    }

    @Test
    void canModifyIsTrueForOwner() {
        Beat existing = new Beat();
        existing.setProducerId(1L);

        assertThat(beatService.canModify(existing, 1L)).isTrue();
    }

    @Test
    void canModifyIsTrueForAdmin() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        Beat existing = new Beat();
        existing.setProducerId(1L);

        assertThat(beatService.canModify(existing, 9L)).isTrue();
    }

    @Test
    void canModifyIsFalseForOtherArtist() {
        User other = new User();
        other.setId(2L);
        other.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        Beat existing = new Beat();
        existing.setProducerId(1L);

        assertThat(beatService.canModify(existing, 2L)).isFalse();
    }

    @Test
    void deleteRemovesBeat() {
        beatService.delete(1L);

        verify(beatRepository).deleteById(1L);
    }
}
