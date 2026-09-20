package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        Collaboration response = new Collaboration();
        response.setId(1L);
        response.setStatus(CollaborationStatus.ACCEPTED);

        when(collaborationService.getById(1L)).thenReturn(collaboration);
        when(collaborationService.canDecide(collaboration, 5L)).thenReturn(true);
        when(collaborationService.decide(eq(1L), eq(CollaborationStatus.ACCEPTED)))
                .thenReturn(response);

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 5L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void decideReturns403WhenRequesterDoesNotOwnTheBeat() throws Exception {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        when(collaborationService.getById(1L)).thenReturn(collaboration);
        when(collaborationService.canDecide(collaboration, 999L)).thenReturn(false);

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 999L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void decideReturns404WhenCollaborationMissing() throws Exception {
        when(collaborationService.getById(1L)).thenReturn(null);

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 5L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void decideReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/collaborations/1")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturns204ForAParty() throws Exception {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        when(collaborationService.getById(1L)).thenReturn(collaboration);
        when(collaborationService.canDelete(collaboration, 2L)).thenReturn(true);

        mockMvc.perform(delete("/api/collaborations/1").requestAttr("userId", 2L))
                .andExpect(status().isNoContent());

        verify(collaborationService).delete(1L);
    }

    @Test
    void deleteReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/collaborations/1"))
                .andExpect(status().isUnauthorized());
    }
}
