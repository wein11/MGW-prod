package com.mgwprod.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.Role;
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
        RegisterRequest request = new RegisterRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.PRODUCER);

        UserResponse response = new UserResponse(1L, "productor@test.com", "DJ Test",
                Role.PRODUCER, null, false, Instant.now(),
                new ProducerProfileDto(null, null, null, null), null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("productor@test.com"))
                .andExpect(jsonPath("$.role").value("PRODUCER"));
    }

    @Test
    void registerReturns400WhenEmailIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.PRODUCER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicado@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.ARTIST);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("duplicado@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
