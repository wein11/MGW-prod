package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.service.ChallengeService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeController.class)
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChallengeService challengeService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createChallengeReturns201WhenAuthenticated() throws Exception {
        Challenge request = new Challenge();
        request.setTitle("Creamos el próximo hit de RKT");
        request.setGenre("RKT");
        request.setBpm(100);
        request.setDeadline(Instant.now().plusSeconds(604800));
        request.setGuestArtistId(2L);

        Challenge response = new Challenge();
        response.setId(1L);
        response.setTitle("Creamos el próximo hit de RKT");

        when(challengeService.canCreateChallenge(1L)).thenReturn(true);
        when(challengeService.userExists(2L)).thenReturn(true);
        when(challengeService.isArtist(2L)).thenReturn(true);
        when(challengeService.create(eq(1L), any(Challenge.class))).thenReturn(response);

        mockMvc.perform(post("/api/challenges")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Creamos el próximo hit de RKT"));
    }

    @Test
    void createChallengeReturns401WhenNotAuthenticated() throws Exception {
        Challenge request = new Challenge();
        request.setTitle("Creamos el próximo hit de RKT");
        request.setGenre("RKT");
        request.setBpm(100);
        request.setDeadline(Instant.now().plusSeconds(604800));
        request.setGuestArtistId(2L);

        mockMvc.perform(post("/api/challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createChallengeReturns403WhenRequesterCannotCreateChallenges() throws Exception {
        Challenge request = new Challenge();
        request.setTitle("Creamos el próximo hit de RKT");
        request.setGenre("RKT");
        request.setBpm(100);
        request.setDeadline(Instant.now().plusSeconds(604800));
        request.setGuestArtistId(2L);

        when(challengeService.canCreateChallenge(1L)).thenReturn(false);

        mockMvc.perform(post("/api/challenges")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listChallengesReturns200() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(1L);

        when(challengeService.list()).thenReturn(java.util.List.of(challenge));

        mockMvc.perform(get("/api/challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getChallengeReturns200WhenExists() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setTitle("Creamos el próximo hit de RKT");

        when(challengeService.getById(1L)).thenReturn(challenge);

        mockMvc.perform(get("/api/challenges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Creamos el próximo hit de RKT"));
    }

    @Test
    void opportunityPickReturns200WhenRequesterIsGuestArtist() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);
        Challenge response = new Challenge();
        response.setId(100L);
        response.setOpportunityPickSubmissionId(7L);

        when(challengeService.getById(100L)).thenReturn(challenge);
        when(challengeService.isGuestArtist(challenge, 99L)).thenReturn(true);
        when(challengeService.setOpportunityPick(challenge, 7L)).thenReturn(response);

        mockMvc.perform(put("/api/challenges/100/opportunity-pick")
                        .requestAttr("userId", 99L)
                        .param("submissionId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunityPickSubmissionId").value(7));
    }

    @Test
    void updateChallengeReturns200ForCreator() throws Exception {
        Challenge existing = new Challenge();
        existing.setId(100L);
        existing.setCreatedBy(1L);
        Challenge response = new Challenge();
        response.setId(100L);
        response.setTitle("Nuevo");

        when(challengeService.getById(100L)).thenReturn(existing);
        when(challengeService.canModify(existing, 1L)).thenReturn(true);
        when(challengeService.isClosed(100L)).thenReturn(false);
        when(challengeService.update(eq(100L), any(Challenge.class))).thenReturn(response);

        mockMvc.perform(put("/api/challenges/100")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo"));
    }

    @Test
    void updateChallengeReturns403WhenAlreadyClosed() throws Exception {
        Challenge existing = new Challenge();
        existing.setId(100L);
        existing.setCreatedBy(1L);

        when(challengeService.getById(100L)).thenReturn(existing);
        when(challengeService.canModify(existing, 1L)).thenReturn(true);
        when(challengeService.isClosed(100L)).thenReturn(true);

        mockMvc.perform(put("/api/challenges/100")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteChallengeReturns204ForCreator() throws Exception {
        Challenge existing = new Challenge();
        existing.setId(100L);
        existing.setCreatedBy(1L);
        when(challengeService.getById(100L)).thenReturn(existing);
        when(challengeService.canModify(existing, 1L)).thenReturn(true);
        when(challengeService.isClosed(100L)).thenReturn(false);

        mockMvc.perform(delete("/api/challenges/100").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        verify(challengeService).delete(100L);
    }

    @Test
    void deleteChallengeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/challenges/100"))
                .andExpect(status().isUnauthorized());
    }
}
