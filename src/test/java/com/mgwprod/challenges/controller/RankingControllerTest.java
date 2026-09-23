package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.RankingEntry;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeResultService challengeResultService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void rankingReturns200SortedByPoints() throws Exception {
        when(challengeResultService.ranking()).thenReturn(java.util.List.of(new RankingEntry(12L, 500)));

        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].producerId").value(12))
                .andExpect(jsonPath("$[0].totalPoints").value(500));
    }

    @Test
    void resultsReturns200FilteredByProducerId() throws Exception {
        when(challengeResultService.listResults(11L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/challenges/results").param("producerId", "11"))
                .andExpect(status().isOk());
    }
}
