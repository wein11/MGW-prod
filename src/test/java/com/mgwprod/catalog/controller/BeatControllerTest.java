package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.service.BeatService;
import com.mgwprod.users.exception.ForbiddenOperationException;
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

@WebMvcTest(BeatController.class)
class BeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BeatService beatService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createBeatReturns201WhenAuthenticatedAsProducer() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        Beat response = new Beat();
        response.setId(1L);
        response.setProducerId(1L);
        response.setTitle("Trap Beat");

        when(beatService.create(eq(1L), any(Beat.class))).thenReturn(response);

        mockMvc.perform(post("/api/beats")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PRODUCER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Trap Beat"));
    }

    @Test
    void createBeatReturns401WhenNotAuthenticated() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        mockMvc.perform(post("/api/beats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBeatReturns403WhenAuthenticatedAsArtist() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        when(beatService.create(eq(2L), any(Beat.class)))
                .thenThrow(new ForbiddenOperationException("Solo un productor puede publicar beats"));

        mockMvc.perform(post("/api/beats")
                        .requestAttr("userId", 2L)
                        .requestAttr("userRole", "ARTIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listBeatsReturns200WithFilteredResults() throws Exception {
        Beat beat = new Beat();
        beat.setId(1L);
        beat.setGenre("Trap");
        beat.setBpm(140);

        when(beatService.list("Trap", null, null)).thenReturn(java.util.List.of(beat));

        mockMvc.perform(get("/api/beats").param("genre", "Trap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre").value("Trap"));
    }

    @Test
    void getBeatByIdReturns200WhenExists() throws Exception {
        Beat beat = new Beat();
        beat.setId(1L);
        beat.setTitle("Trap Beat");

        when(beatService.getById(1L)).thenReturn(beat);

        mockMvc.perform(get("/api/beats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Trap Beat"));
    }

    @Test
    void getBeatByIdReturns404WhenMissing() throws Exception {
        when(beatService.getById(99L))
                .thenThrow(new com.mgwprod.catalog.exception.BeatNotFoundException(99L));

        mockMvc.perform(get("/api/beats/99"))
                .andExpect(status().isNotFound());
    }
}
