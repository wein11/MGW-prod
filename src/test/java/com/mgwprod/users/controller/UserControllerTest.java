package com.mgwprod.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void getUserReturns200WithUserData() throws Exception {
        User response = new User();
        response.setId(1L);
        response.setDisplayName("DJ Test");
        response.setRole(Role.ARTIST);
        response.setCity("Buenos Aires");

        when(userService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("DJ Test"));
    }

    @Test
    void getUserReturns404WhenUserDoesNotExist() throws Exception {
        when(userService.getById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfileReturns200WithArtistProfile() throws Exception {
        ArtistProfile profile = new ArtistProfile();
        profile.setGenres("RKT");

        when(userService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").value("RKT"));
    }

    @Test
    void updateUserReturns200WhenOwnerEditsOwnProfile() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        User response = new User();
        response.setId(1L);
        response.setDisplayName("Nuevo Nombre");
        response.setRole(Role.ARTIST);

        when(userService.updateUser(eq(1L), eq(1L), any(User.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nuevo Nombre"));
    }

    @Test
    void updateUserReturns401WhenNoUserIdAttribute() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateArtistProfileReturns200WhenOwnerEditsOwnProfile() throws Exception {
        ArtistProfile request = new ArtistProfile();
        request.setGenres("RKT,Trap");
        request.setBpmMin(120);
        request.setBpmMax(140);
        request.setExperienceLevel("intermedio");

        ArtistProfile response = new ArtistProfile();
        response.setGenres("RKT,Trap");
        response.setBpmMin(120);
        response.setBpmMax(140);
        response.setExperienceLevel("intermedio");

        when(userService.updateArtistProfile(eq(1L), eq(1L), any(ArtistProfile.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1/artist-profile")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").value("RKT,Trap"))
                .andExpect(jsonPath("$.bpmMin").value(120))
                .andExpect(jsonPath("$.bpmMax").value(140))
                .andExpect(jsonPath("$.experienceLevel").value("intermedio"));
    }
}
