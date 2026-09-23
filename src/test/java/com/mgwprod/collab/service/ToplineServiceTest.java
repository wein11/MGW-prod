package com.mgwprod.collab.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToplineServiceTest {

    @Mock
    private ToplineRepository toplineRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BeatRepository beatRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private ToplineService toplineService;

    @Test
    void isArtistReturnsTrueForArtistRole() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThat(toplineService.isArtist(1L)).isTrue();
    }

    @Test
    void isArtistReturnsFalseForOtherRoles() {
        User discografica = new User();
        discografica.setId(1L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(1L)).thenReturn(Optional.of(discografica));

        assertThat(toplineService.isArtist(1L)).isFalse();
    }

    @Test
    void createSavesToplineAndPendingCollaboration() {
        Beat beat = new Beat();
        beat.setId(2L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        Topline topline = new Topline();
        topline.setBeat(beat);
        topline.setAudioUrl("https://soundcloud.com/example/topline");

        when(toplineRepository.save(any(Topline.class))).thenAnswer(invocation -> {
            Topline saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(collaborationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Topline saved = toplineService.create(1L, topline);

        assertThat(saved.getArtistId()).isEqualTo(1L);

        ArgumentCaptor<com.mgwprod.collab.model.Collaboration> captor =
                ArgumentCaptor.forClass(com.mgwprod.collab.model.Collaboration.class);
        verify(collaborationRepository).save(captor.capture());
        assertThat(captor.getValue().getToplineId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(CollaborationStatus.PENDING);
    }

    @Test
    void createReturnsNullWhenBeatMissing() {
        Beat beat = new Beat();
        beat.setId(2L);
        when(beatRepository.findById(2L)).thenReturn(Optional.empty());

        Topline topline = new Topline();
        topline.setBeat(beat);

        assertThat(toplineService.create(1L, topline)).isNull();
        verify(toplineRepository, never()).save(any());
    }

    @Test
    void createCallsRecordProductionBeforeSaving() {
        Beat beat = new Beat();
        beat.setId(2L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        Topline topline = new Topline();
        topline.setBeat(beat);
        topline.setAudioUrl("https://soundcloud.com/example/topline");

        when(toplineRepository.save(any(Topline.class))).thenAnswer(invocation -> {
            Topline saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(collaborationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        toplineService.create(1L, topline);

        verify(subscriptionService).recordProduction(1L);
    }

    @Test
    void isAtProductionLimitDelegatesToSubscriptionService() {
        when(subscriptionService.isAtProductionLimit(1L)).thenReturn(true);

        assertThat(toplineService.isAtProductionLimit(1L)).isTrue();
    }

    @Test
    void updateChangesAudioUrl() {
        Topline existing = new Topline();
        existing.setId(1L);
        existing.setArtistId(1L);
        existing.setAudioUrl("https://old.url");
        when(toplineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(toplineRepository.save(any(Topline.class))).thenAnswer(inv -> inv.getArgument(0));

        Topline request = new Topline();
        request.setAudioUrl("https://new.url");

        Topline updated = toplineService.update(1L, request);

        assertThat(updated.getAudioUrl()).isEqualTo("https://new.url");
    }

    @Test
    void canModifyIsTrueForOwner() {
        Topline existing = new Topline();
        existing.setArtistId(1L);

        assertThat(toplineService.canModify(existing, 1L)).isTrue();
    }

    @Test
    void canModifyIsTrueForAdmin() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        Topline existing = new Topline();
        existing.setArtistId(1L);

        assertThat(toplineService.canModify(existing, 9L)).isTrue();
    }

    @Test
    void canModifyIsFalseForOtherArtist() {
        User other = new User();
        other.setId(2L);
        other.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        Topline existing = new Topline();
        existing.setArtistId(1L);

        assertThat(toplineService.canModify(existing, 2L)).isFalse();
    }

    @Test
    void deleteRemovesTopline() {
        toplineService.delete(1L);

        verify(toplineRepository).deleteById(1L);
    }
}
