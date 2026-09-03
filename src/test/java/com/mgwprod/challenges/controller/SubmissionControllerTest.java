package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.service.SubmissionService;
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

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createSubmissionReturns201WhenAuthenticated() throws Exception {
        Submission request = new Submission();
        request.setAudioUrl("https://soundcloud.com/example/submission");

        Submission response = new Submission();
        response.setId(1L);
        response.setChallengeId(1L);
        response.setProducerId(2L);

        when(submissionService.create(eq(1L), eq(2L), any(Submission.class))).thenReturn(response);

        mockMvc.perform(post("/api/challenges/1/submissions")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.producerId").value(2));
    }

    @Test
    void createSubmissionReturns401WhenNotAuthenticated() throws Exception {
        Submission request = new Submission();
        request.setAudioUrl("https://soundcloud.com/example/submission");

        mockMvc.perform(post("/api/challenges/1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSubmissionsReturns200() throws Exception {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setChallengeId(1L);

        when(submissionService.listByChallenge(1L)).thenReturn(java.util.List.of(submission));

        mockMvc.perform(get("/api/challenges/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].challengeId").value(1));
    }
}
