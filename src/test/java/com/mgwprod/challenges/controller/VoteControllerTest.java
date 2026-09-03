package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.service.VoteService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoteService voteService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createVoteReturns201WhenAuthenticated() throws Exception {
        Vote request = new Vote();
        request.setScore(9);

        Vote response = new Vote();
        response.setId(1L);
        response.setSubmissionId(1L);
        response.setVoterId(2L);
        response.setScore(9);

        when(voteService.create(eq(1L), eq(2L), any(Vote.class))).thenReturn(response);

        mockMvc.perform(post("/api/submissions/1/votes")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(9));
    }

    @Test
    void createVoteReturns401WhenNotAuthenticated() throws Exception {
        Vote request = new Vote();
        request.setScore(9);

        mockMvc.perform(post("/api/submissions/1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
