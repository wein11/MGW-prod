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
import static org.mockito.Mockito.when;
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
        // Cuerpo válido a propósito: como el controller usa @Valid @RequestBody (mandato
        // del spec), la validación del body corre antes que el chequeo de userId. Con un
        // body incompleto el endpoint devolvería 400 por validación y nunca llegaría al
        // 401 que queremos ejercitar. Con el body completo, validación pasa y salta el 401.
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
        Challenge response = new Challenge();
        response.setId(100L);
        response.setOpportunityPickSubmissionId(7L);

        when(challengeService.setOpportunityPick(eq(100L), eq(99L), eq(7L))).thenReturn(response);

        mockMvc.perform(put("/api/challenges/100/opportunity-pick")
                        .requestAttr("userId", 99L)
                        .param("submissionId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunityPickSubmissionId").value(7));
    }
}
