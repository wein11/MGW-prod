package com.mgwprod.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void registerReturns201WithUserData() throws Exception {
        User response = new User();
        response.setId(1L);
        response.setEmail("productor@test.com");
        response.setDisplayName("DJ Test");
        response.setRole(Role.PRODUCER);
        response.setCreatedAt(Instant.now());

        when(authService.register(any(User.class))).thenReturn(response);

        String requestJson = """
                {"email":"productor@test.com","password":"supersecret123","displayName":"DJ Test","role":"PRODUCER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("productor@test.com"))
                .andExpect(jsonPath("$.role").value("PRODUCER"))
                .andExpect(jsonPath("$.isAdmin").value(false))
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerReturns400WhenEmailIsBlank() throws Exception {
        String requestJson = """
                {"email":"","password":"supersecret123","displayName":"DJ Test","role":"PRODUCER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(User.class)))
                .thenThrow(new EmailAlreadyExistsException("duplicado@test.com"));

        String requestJson = """
                {"email":"duplicado@test.com","password":"supersecret123","displayName":"DJ Test","role":"ARTIST"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");

        LoginResponse response = new LoginResponse("some-token-123", 1L, "DJ Test", Role.PRODUCER);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("some-token-123"));
    }

    @Test
    void loginReturns401WithWrongCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
