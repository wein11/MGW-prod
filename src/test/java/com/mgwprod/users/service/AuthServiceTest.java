package com.mgwprod.users.service;

import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private SessionRepository sessionRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, artistProfileRepository,
                sessionRepository, new PasswordHasher());
    }

    @Test
    void registerCreatesArtistProfileForArtistRole() {
        User incoming = new User();
        incoming.setEmail("artista@test.com");
        incoming.setPassword("supersecret123");
        incoming.setDisplayName("DJ Test");
        incoming.setRole(Role.ARTIST);

        when(userRepository.existsByEmail("artista@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User response = authService.register(incoming);

        assertEquals("artista@test.com", response.getEmail());
        assertEquals(Role.ARTIST, response.getRole());
        verify(artistProfileRepository).save(any());
    }

    @Test
    void registerDoesNotCreateArtistProfileForDiscograficaRole() {
        User incoming = new User();
        incoming.setEmail("discografica@test.com");
        incoming.setPassword("supersecret123");
        incoming.setDisplayName("Sello Test");
        incoming.setRole(Role.DISCOGRAFICA);

        when(userRepository.existsByEmail("discografica@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User response = authService.register(incoming);

        assertEquals(Role.DISCOGRAFICA, response.getRole());
        verify(artistProfileRepository, never()).save(any());
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
        user.setEmail("artista@test.com");
        user.setPasswordHash(hasher.hash("supersecret123"));
        user.setDisplayName("DJ Test");
        user.setRole(Role.ARTIST);

        when(userRepository.findByEmail("artista@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session session = authService.login("artista@test.com", "supersecret123");

        assertEquals(1L, session.getUser().getId());
        assertEquals(Role.ARTIST, session.getUser().getRole());
    }

    @Test
    void loginThrowsWithWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        User user = new User();
        user.setId(1L);
        user.setEmail("artista@test.com");
        user.setPasswordHash(hasher.hash("supersecret123"));
        user.setRole(Role.ARTIST);

        when(userRepository.findByEmail("artista@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("artista@test.com", "wrongpassword"));
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
                () -> authService.login("artista@test.com", ""));
    }
}
