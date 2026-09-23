package com.mgwprod.users.security;

import com.mgwprod.users.model.Session;
import com.mgwprod.users.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Optional;

// Se ejecuta antes de cada método de controller (se registra en WebConfig). Lee el
// header "Authorization: Bearer <token>", busca ese token en la tabla de sesiones y,
// si es válido, guarda el userId/role del que llama como atributos del request para
// que los controllers los lean con @RequestAttribute.
public class SessionAuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String USER_ROLE_ATTRIBUTE = "userRole";

    private final SessionRepository sessionRepository;

    public SessionAuthInterceptor(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Devolver true = el request sigue hacia el controller; false = se corta acá (401).
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        // Sin header o de otro tipo: deja pasar el request sin autenticar. El
        // controller es quien decide si ese endpoint necesita login (chequeando si
        // USER_ID_ATTRIBUTE quedó seteado) — este interceptor solo resuelve quién es
        // el que llama, no rechaza requests por su cuenta.
        if (header == null || !header.startsWith("Bearer ")) {
            return true;
        }

        String token = header.substring("Bearer ".length());
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);

        // Acá sí se manda un header Bearer pero es inválido/expirado: se rechaza
        // directamente, porque un token malo es distinto a no mandar ninguno.
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
