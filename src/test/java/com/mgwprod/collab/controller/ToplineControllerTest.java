package com.mgwprod.collab.controller;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        Topline request = new Topline();
        request.setBeatId(2L);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        Topline response = new Topline();
        response.setId(10L);
        response.setArtistId(1L);
        response.setBeatId(2L);

        when(toplineService.create(eq(1L), any(Topline.class))).thenReturn(response);

        mockMvc.perform(post("/api/toplines")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beatId").value(2));
    }

    @Test
    void createToplineReturns401WhenNotAuthenticated() throws Exception {
        Topline request = new Topline();
        request.setBeatId(2L);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        mockMvc.perform(post("/api/toplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listToplinesReturns200() throws Exception {
        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);

        when(toplineService.list(2L, null)).thenReturn(java.util.List.of(topline));

        mockMvc.perform(get("/api/toplines").param("beatId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beatId").value(2));
    }
}
