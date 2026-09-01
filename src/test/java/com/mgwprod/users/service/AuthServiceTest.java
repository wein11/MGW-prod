package com.mgwprod.users.service;

import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.Session;
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
        User incoming = new User();
        incoming.setEmail("productor@test.com");
        incoming.setPassword("supersecret123");
        incoming.setDisplayName("DJ Test");
        incoming.setRole(Role.PRODUCER);

        when(userRepository.existsByEmail("productor@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User response = authService.register(incoming);

        assertEquals("productor@test.com", response.getEmail());
        assertEquals(Role.PRODUCER, response.getRole());
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        User incoming = new User();
        incoming.setEmail("duplicado@test.com");
        incoming.setPassword("supersecret123");
        incoming.setDisplayName("DJ Test");
        incoming.setRole(Role.ARTIST);

        when(userRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(incoming));
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

        when(userRepository.findByEmail("productor@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session session = authService.login("productor@test.com", "supersecret123");

        assertEquals(1L, session.getUser().getId());
        assertEquals(Role.PRODUCER, session.getUser().getRole());
    }

    @Test
    void loginThrowsWithWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setPasswordHash(hasher.hash("supersecret123"));
        user.setRole(Role.PRODUCER);

        when(userRepository.findByEmail("productor@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("productor@test.com", "wrongpassword"));
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("noexiste@test.com", "supersecret123"));
    }

    @Test
    void loginThrowsWhenPasswordBlank() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("productor@test.com", ""));
    }
}
