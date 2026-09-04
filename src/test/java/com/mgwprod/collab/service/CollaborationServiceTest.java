package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private ToplineService toplineService;

    @Mock
    private BeatRepository beatRepository;

    @InjectMocks
    private CollaborationService collaborationService;

    @Test
    void decideAcceptsWhenRequesterOwnsTheBeat() {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        collaboration.setToplineId(10L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collaboration));

        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Beat beat = new Beat();
        beat.setId(2L);
        beat.setProducerId(5L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        when(collaborationRepository.save(collaboration)).thenReturn(collaboration);

        Collaboration result = collaborationService.decide(1L, 5L, CollaborationStatus.ACCEPTED);

        assertThat(result.getStatus()).isEqualTo(CollaborationStatus.ACCEPTED);
        assertThat(result.getDecidedAt()).isNotNull();
    }

    @Test
    void decideThrowsWhenRequesterDoesNotOwnTheBeat() {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        collaboration.setToplineId(10L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collaboration));

        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Beat beat = new Beat();
        beat.setId(2L);
        beat.setProducerId(5L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        assertThatThrownBy(() -> collaborationService.decide(1L, 999L, CollaborationStatus.ACCEPTED))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
