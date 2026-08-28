package com.mgwprod.users.service;

import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.repository.UserRepository;
import com.mgwprod.users.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProducerProfileRepository producerProfileRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private SessionRepository sessionRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, producerProfileRepository,
                artistProfileRepository, sessionRepository, new PasswordHasher());
    }

    @Test
    void registerCreatesUserWithProducerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.PRODUCER);

        when(userRepository.existsByEmail("productor@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(Instant.now());
            return user;
        });

        UserResponse response = authService.register(request);

        assertEquals("productor@test.com", response.getEmail());
        assertEquals(Role.PRODUCER, response.getRole());
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicado@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.ARTIST);

        when(userRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        PasswordHasher hasher = new PasswordHasher();
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setPasswordHash(hasher.hash("supersecret123"));
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);

        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");

        when(userRepository.findByEmail("productor@test.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertEquals(1L, response.getUserId());
        assertEquals(Role.PRODUCER, response.getRole());
    }

    @Test
    void loginThrowsWithWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setPasswordHash(hasher.hash("supersecret123"));
        user.setRole(Role.PRODUCER);

        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("productor@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@test.com");
        request.setPassword("supersecret123");

        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
