package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private ToplineService toplineService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollaborationService collaborationService;

    @Test
    void canDecideIsTrueWhenRequesterOwnsTheBeat() {
        Topline topline = new Topline();
        topline.setId(10L);
        Beat beat = new Beat();
        beat.setProducerId(5L);
        topline.setBeat(beat);
        when(toplineService.getById(10L)).thenReturn(topline);

        Collaboration collaboration = new Collaboration();
        collaboration.setToplineId(10L);

        assertThat(collaborationService.canDecide(collaboration, 5L)).isTrue();
        assertThat(collaborationService.canDecide(collaboration, 999L)).isFalse();
    }

    @Test
    void decideUpdatesStatusAndDecidedAt() {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collaboration));
        when(collaborationRepository.save(collaboration)).thenReturn(collaboration);

        Collaboration result = collaborationService.decide(1L, CollaborationStatus.ACCEPTED);

        assertThat(result.getStatus()).isEqualTo(CollaborationStatus.ACCEPTED);
        assertThat(result.getDecidedAt()).isNotNull();
    }

    @Test
    void canDeleteIsTrueForTheArtistWhoOwnsTheTopline() {
        Topline topline = new Topline();
        topline.setId(5L);
        topline.setArtistId(2L);
        Beat beat = new Beat();
        beat.setProducerId(3L);
        topline.setBeat(beat);
        when(toplineService.getById(5L)).thenReturn(topline);

        Collaboration collab = new Collaboration();
        collab.setToplineId(5L);

        assertThat(collaborationService.canDelete(collab, 2L)).isTrue();
    }

    @Test
    void canDeleteIsTrueForTheProducerWhoOwnsTheBeat() {
        Topline topline = new Topline();
        topline.setId(5L);
        topline.setArtistId(2L);
        Beat beat = new Beat();
        beat.setProducerId(3L);
        topline.setBeat(beat);
        when(toplineService.getById(5L)).thenReturn(topline);

        Collaboration collab = new Collaboration();
        collab.setToplineId(5L);

        assertThat(collaborationService.canDelete(collab, 3L)).isTrue();
    }

    @Test
    void canDeleteIsFalseWhenRequesterIsNeitherParty() {
        Topline topline = new Topline();
        topline.setId(5L);
        topline.setArtistId(2L);
        Beat beat = new Beat();
        beat.setProducerId(3L);
        topline.setBeat(beat);
        when(toplineService.getById(5L)).thenReturn(topline);

        Collaboration collab = new Collaboration();
        collab.setToplineId(5L);

        User other = new User();
        other.setId(99L);
        other.setRole(Role.ARTIST);
        when(userRepository.findById(99L)).thenReturn(Optional.of(other));

        assertThat(collaborationService.canDelete(collab, 99L)).isFalse();
    }

    @Test
    void canDeleteIsTrueForAdminEvenIfNeitherParty() {
        Topline topline = new Topline();
        topline.setId(5L);
        topline.setArtistId(2L);
        Beat beat = new Beat();
        beat.setProducerId(3L);
        topline.setBeat(beat);
        when(toplineService.getById(5L)).thenReturn(topline);

        Collaboration collab = new Collaboration();
        collab.setToplineId(5L);

        User admin = new User();
        admin.setId(9L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        assertThat(collaborationService.canDelete(collab, 9L)).isTrue();
    }

    @Test
    void deleteRemovesCollaboration() {
        collaborationService.delete(1L);

        verify(collaborationRepository).deleteById(1L);
    }
}
