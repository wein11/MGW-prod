package com.mgwprod.collab.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.service.ToplineService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToplineController.class)
class ToplineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ToplineService toplineService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createToplineReturns201WhenAuthenticated() throws Exception {
        Beat beat = new Beat();
        beat.setId(2L);
        Topline request = new Topline();
        request.setBeat(beat);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        Topline response = new Topline();
        response.setId(10L);
        response.setArtistId(1L);
        response.setBeat(beat);

        when(toplineService.isArtist(1L)).thenReturn(true);
        when(toplineService.create(eq(1L), any(Topline.class))).thenReturn(response);

        mockMvc.perform(post("/api/toplines")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beat.id").value(2));
    }

    @Test
    void createToplineReturns401WhenNotAuthenticated() throws Exception {
        Beat beat = new Beat();
        beat.setId(2L);
        Topline request = new Topline();
        request.setBeat(beat);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        mockMvc.perform(post("/api/toplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createToplineReturns403WhenNotAnArtist() throws Exception {
        Beat beat = new Beat();
        beat.setId(2L);
        Topline request = new Topline();
        request.setBeat(beat);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        when(toplineService.isArtist(1L)).thenReturn(false);

        mockMvc.perform(post("/api/toplines")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listToplinesReturns200() throws Exception {
        Beat beat = new Beat();
        beat.setId(2L);
        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeat(beat);

        when(toplineService.list(2L, null)).thenReturn(java.util.List.of(topline));

        mockMvc.perform(get("/api/toplines").param("beatId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beat.id").value(2));
    }

    @Test
    void updateToplineReturns200ForOwner() throws Exception {
        Topline existing = new Topline();
        existing.setId(1L);
        existing.setArtistId(1L);
        Topline response = new Topline();
        response.setId(1L);
        response.setAudioUrl("https://new.url");

        when(toplineService.getById(1L)).thenReturn(existing);
        when(toplineService.canModify(existing, 1L)).thenReturn(true);
        when(toplineService.update(eq(1L), any(Topline.class))).thenReturn(response);

        mockMvc.perform(put("/api/toplines/1")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioUrl\":\"https://new.url\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioUrl").value("https://new.url"));
    }

    @Test
    void deleteToplineReturns204ForOwner() throws Exception {
        Topline existing = new Topline();
        existing.setId(1L);
        existing.setArtistId(1L);
        when(toplineService.getById(1L)).thenReturn(existing);
        when(toplineService.canModify(existing, 1L)).thenReturn(true);

        mockMvc.perform(delete("/api/toplines/1").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        verify(toplineService).delete(1L);
    }

    @Test
    void deleteToplineReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/toplines/1"))
                .andExpect(status().isUnauthorized());
    }
}
