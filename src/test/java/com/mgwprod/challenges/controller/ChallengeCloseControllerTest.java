package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeCloseController.class)
class ChallengeCloseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeResultService challengeResultService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void closeReturns200WithResultsWhenRequesterIsAdmin() throws Exception {
        ChallengeResult result = new ChallengeResult();
        result.setRank(1);
        result.setPointsAwarded(500);

        when(challengeResultService.close(eq(100L), eq(1L))).thenReturn(java.util.List.of(result));

        mockMvc.perform(put("/api/challenges/100/close").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pointsAwarded").value(500));
    }

    @Test
    void closeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/challenges/100/close"))
                .andExpect(status().isUnauthorized());
    }
}
