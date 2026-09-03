package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ToplineService toplineService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createSavesCommentWhenToplineExists() {
        Topline topline = new Topline();
        topline.setId(10L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Comment comment = new Comment();
        comment.setText("Qué voz");
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment saved = commentService.create(10L, 2L, comment);

        assertThat(saved.getToplineId()).isEqualTo(10L);
        assertThat(saved.getAuthorId()).isEqualTo(2L);
    }
}
