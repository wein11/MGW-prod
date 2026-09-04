package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.service.BeatCommentService;
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

@WebMvcTest(BeatCommentController.class)
class BeatCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BeatCommentService beatCommentService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createCommentReturns201WhenAuthenticated() throws Exception {
        BeatComment request = new BeatComment();
        request.setText("Buenísimo");

        BeatComment response = new BeatComment();
        response.setId(1L);
        response.setBeatId(1L);
        response.setAuthorId(2L);
        response.setText("Buenísimo");

        when(beatCommentService.create(eq(1L), eq(2L), any(BeatComment.class))).thenReturn(response);

        mockMvc.perform(post("/api/beats/1/comments")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Buenísimo"));
    }

    @Test
    void createCommentReturns401WhenNotAuthenticated() throws Exception {
        BeatComment request = new BeatComment();
        request.setText("Buenísimo");

        mockMvc.perform(post("/api/beats/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCommentsReturns200() throws Exception {
        BeatComment comment = new BeatComment();
        comment.setText("Buenísimo");

        when(beatCommentService.listByBeat(1L)).thenReturn(java.util.List.of(comment));

        mockMvc.perform(get("/api/beats/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Buenísimo"));
    }
}
