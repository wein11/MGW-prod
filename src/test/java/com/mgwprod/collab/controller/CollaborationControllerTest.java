package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import com.mgwprod.users.exception.ForbiddenOperationException;
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

@WebMvcTest(CollaborationController.class)
class CollaborationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollaborationService collaborationService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void decideReturns200WhenRequesterOwnsTheBeat() throws Exception {
        Collaboration response = new Collaboration();
        response.setId(1L);
        response.setStatus(CollaborationStatus.ACCEPTED);

        when(collaborationService.decide(eq(1L), eq(5L), eq(CollaborationStatus.ACCEPTED)))
                .thenReturn(response);

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 5L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void decideReturns403WhenRequesterDoesNotOwnTheBeat() throws Exception {
        when(collaborationService.decide(eq(1L), eq(999L), eq(CollaborationStatus.ACCEPTED)))
                .thenThrow(new ForbiddenOperationException("Solo el productor dueño del beat puede decidir esta colaboración"));

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 999L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void decideReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/collaborations/1")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isUnauthorized());
    }
}
