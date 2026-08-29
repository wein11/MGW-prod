package com.mgwprod.users.security;

import com.mgwprod.users.model.Session;
import com.mgwprod.users.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Optional;

public class SessionAuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String USER_ROLE_ATTRIBUTE = "userRole";

    private final SessionRepository sessionRepository;

    public SessionAuthInterceptor(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return true;
        }

        String token = header.substring("Bearer ".length());
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);

        if (sessionOpt.isEmpty() || sessionOpt.get().getExpiresAt().isBefore(Instant.now())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Session session = sessionOpt.get();
        request.setAttribute(USER_ID_ATTRIBUTE, session.getUser().getId());
        request.setAttribute(USER_ROLE_ATTRIBUTE, session.getUser().getRole().name());
        return true;
    }
}
