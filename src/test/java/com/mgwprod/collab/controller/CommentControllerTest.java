package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.service.CommentService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createCommentReturns201WhenAuthenticated() throws Exception {
        Comment request = new Comment();
        request.setText("Qué voz");

        Comment response = new Comment();
        response.setId(1L);
        response.setToplineId(10L);
        response.setAuthorId(2L);
        response.setText("Qué voz");

        when(commentService.create(eq(10L), eq(2L), any(Comment.class))).thenReturn(response);

        mockMvc.perform(post("/api/toplines/10/comments")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Qué voz"));
    }

    @Test
    void createCommentReturns401WhenNotAuthenticated() throws Exception {
        Comment request = new Comment();
        request.setText("Qué voz");

        mockMvc.perform(post("/api/toplines/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCommentsReturns200() throws Exception {
        Comment comment = new Comment();
        comment.setText("Qué voz");

        when(commentService.listByTopline(10L)).thenReturn(java.util.List.of(comment));

        mockMvc.perform(get("/api/toplines/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Qué voz"));
    }
}
