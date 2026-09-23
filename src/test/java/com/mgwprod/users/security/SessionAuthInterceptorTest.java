package com.mgwprod.users.security;

import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthInterceptorTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private SessionAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new SessionAuthInterceptor(sessionRepository);
    }

    @Test
    void allowsRequestWithoutAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void setsUserAttributesForValidToken() {
        User user = new User();
        user.setId(42L);
        user.setRole(Role.ARTIST);

        Session session = new Session();
        session.setUser(user);
        session.setToken("valid-token");
        session.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(sessionRepository.findByToken("valid-token")).thenReturn(Optional.of(session));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(request).setAttribute(SessionAuthInterceptor.USER_ID_ATTRIBUTE, 42L);
        verify(request).setAttribute(SessionAuthInterceptor.USER_ROLE_ATTRIBUTE, "ARTIST");
    }

    @Test
    void rejectsExpiredToken() {
        User user = new User();
        user.setId(42L);
        user.setRole(Role.ARTIST);

        Session session = new Session();
        session.setUser(user);
        session.setToken("expired-token");
        session.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(sessionRepository.findByToken("expired-token")).thenReturn(Optional.of(session));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
