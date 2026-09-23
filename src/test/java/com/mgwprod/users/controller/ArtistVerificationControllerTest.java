package com.mgwprod.users.controller;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistVerificationController.class)
class ArtistVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void verifyReturns200WhenRequesterIsAdmin() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        User artist = new User();
        artist.setId(2L);
        artist.setRole(Role.ARTIST);
        ArtistProfile profile = new ArtistProfile();
        profile.setVerified(true);

        when(userService.getById(1L)).thenReturn(admin);
        when(userService.isAdmin(admin)).thenReturn(true);
        when(userService.getById(2L)).thenReturn(artist);
        when(userService.isArtist(artist)).thenReturn(true);
        when(userService.verifyArtist(2L)).thenReturn(profile);

        mockMvc.perform(put("/api/artists/2/verify").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void verifyReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/artists/2/verify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyReturns403WhenRequesterIsNotAdmin() throws Exception {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setRole(Role.ARTIST);
        when(userService.getById(1L)).thenReturn(notAdmin);
        when(userService.isAdmin(notAdmin)).thenReturn(false);

        mockMvc.perform(put("/api/artists/2/verify").requestAttr("userId", 1L))
                .andExpect(status().isForbidden());
    }
}
