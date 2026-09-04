package com.mgwprod.catalog.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.repository.BeatCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeatCommentServiceTest {

    @Mock
    private BeatCommentRepository beatCommentRepository;

    @Mock
    private BeatService beatService;

    @InjectMocks
    private BeatCommentService beatCommentService;

    @Test
    void createSavesCommentWhenBeatExists() {
        Beat beat = new Beat();
        beat.setId(1L);
        when(beatService.getById(1L)).thenReturn(beat);

        BeatComment comment = new BeatComment();
        comment.setText("Buenísimo");
        when(beatCommentRepository.save(any(BeatComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BeatComment saved = beatCommentService.create(1L, 2L, comment);

        assertThat(saved.getBeatId()).isEqualTo(1L);
        assertThat(saved.getAuthorId()).isEqualTo(2L);
    }
}
