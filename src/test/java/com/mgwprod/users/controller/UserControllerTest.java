package com.mgwprod.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getUserReturns200WithUserData() throws Exception {
        UserResponse response = new UserResponse(1L, "productor@test.com", "DJ Test",
                Role.PRODUCER, "Buenos Aires", false, Instant.now(),
                new ProducerProfileDto("RKT", 90, 140, "intermediate"), null);

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
}
