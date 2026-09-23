package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.challenges.service.ChallengeService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    private ChallengeService challengeService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void closeReturns200WithResultsWhenRequesterIsAdmin() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        ChallengeResult result = new ChallengeResult();
        result.setRank(1);
        result.setPointsAwarded(500);

        when(challengeResultService.isAdmin(1L)).thenReturn(true);
        when(challengeService.getById(100L)).thenReturn(challenge);
        when(challengeResultService.close(challenge)).thenReturn(java.util.List.of(result));

        mockMvc.perform(put("/api/challenges/100/close").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pointsAwarded").value(500));
    }

    @Test
    void closeReturns403WhenRequesterIsNotAdmin() throws Exception {
        when(challengeResultService.isAdmin(1L)).thenReturn(false);

        mockMvc.perform(put("/api/challenges/100/close").requestAttr("userId", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void closeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/challenges/100/close"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void closeReturns403WhenChallengeIsAlreadyClosed() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        when(challengeResultService.isAdmin(1L)).thenReturn(true);
        when(challengeService.getById(100L)).thenReturn(challenge);
        when(challengeService.isClosed(100L)).thenReturn(true);

        mockMvc.perform(put("/api/challenges/100/close").requestAttr("userId", 1L))
                .andExpect(status().isForbidden());
    }
}
