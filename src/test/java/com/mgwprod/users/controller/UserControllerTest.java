package com.mgwprod.users.controller;

import tools.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        when(userService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfileReturns200WithArtistProfile() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ARTIST);
        ArtistProfile profile = new ArtistProfile();
        profile.setGenres("RKT");

        when(userService.getById(1L)).thenReturn(user);
        when(userService.isArtist(user)).thenReturn(true);
        when(userService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").value("RKT"));
    }

    @Test
    void updateUserReturns200WhenOwnerEditsOwnProfile() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        User existing = new User();
        existing.setId(1L);
        User response = new User();
        response.setId(1L);
        response.setDisplayName("Nuevo Nombre");
        response.setRole(Role.ARTIST);

        when(userService.isOwner(1L, 1L)).thenReturn(true);
        when(userService.getById(1L)).thenReturn(existing);
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(response);

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
    void updateUserReturns403WhenEditingSomeoneElse() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        when(userService.isOwner(1L, 2L)).thenReturn(false);

        mockMvc.perform(put("/api/users/1")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateArtistProfileReturns200WhenOwnerEditsOwnProfile() throws Exception {
        ArtistProfile request = new ArtistProfile();
        request.setGenres("RKT,Trap");
        request.setBpmMin(120);
        request.setBpmMax(140);
        request.setExperienceLevel("intermedio");

        User existing = new User();
        existing.setId(1L);
        existing.setRole(Role.ARTIST);

        ArtistProfile response = new ArtistProfile();
        response.setGenres("RKT,Trap");
        response.setBpmMin(120);
        response.setBpmMax(140);
        response.setExperienceLevel("intermedio");

        when(userService.isOwner(1L, 1L)).thenReturn(true);
        when(userService.getById(1L)).thenReturn(existing);
        when(userService.isArtist(existing)).thenReturn(true);
        when(userService.updateArtistProfile(eq(1L), any(ArtistProfile.class))).thenReturn(response);

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

    @Test
    void deleteUserReturns204ForOwner() throws Exception {
        User target = new User();
        target.setId(1L);
        when(userService.getById(1L)).thenReturn(target);
        when(userService.isOwner(1L, 1L)).thenReturn(true);
        when(userService.delete(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void deleteUserReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUserReturns409WhenUserHasContent() throws Exception {
        User target = new User();
        target.setId(1L);
        when(userService.getById(1L)).thenReturn(target);
        when(userService.isOwner(1L, 1L)).thenReturn(true);
        when(userService.delete(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/1").requestAttr("userId", 1L))
                .andExpect(status().isConflict());
    }
}
