package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ToplineService toplineService;

    @Test
    void createSavesToplineAndPendingCollaboration() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setId(2L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        Topline topline = new Topline();
        topline.setBeatId(2L);
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
    void createThrowsWhenUserIsNotArtist() {
        User discografica = new User();
        discografica.setId(1L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(1L)).thenReturn(Optional.of(discografica));

        Topline topline = new Topline();
        topline.setBeatId(2L);

        assertThatThrownBy(() -> toplineService.create(1L, topline))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
