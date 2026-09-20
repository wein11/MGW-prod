package com.mgwprod.users.service;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.repository.UserRepository;
import com.mgwprod.users.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// Maneja los dos endpoints de auth: registro y login. Está separado de UserService
// (que se encarga de leer/actualizar un usuario ya existente) porque auth es un
// problema aparte, con sus propias dependencias (PasswordHasher, SessionRepository).
@Service
public class AuthService {

    private static final long SESSION_DURATION_HOURS = 24;

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository,
                        ArtistProfileRepository artistProfileRepository,
                        SessionRepository sessionRepository,
                        PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHasher = passwordHasher;
    }

    // El controller la usa para devolver 409 antes de registrar.
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // AuthController ya corrió los chequeos de 400 y el chequeo de 409 (emailExists)
    // antes de llamar acá — para cuando llegamos a este punto, `incoming` es confiable.
    @Transactional
    public User register(User incoming) {
        User user = new User();
        user.setEmail(incoming.getEmail());
        // Nunca se persiste la contraseña en texto plano, solo su hash.
        // `incoming.getPassword()` es el campo @Transient que la trajo desde el JSON del request.
        user.setPasswordHash(passwordHasher.hash(incoming.getPassword()));
        user.setPassword(incoming.getPassword());
        user.setDisplayName(incoming.getDisplayName());
        user.setRole(incoming.getRole());
        user.setCity(incoming.getCity());
        user = userRepository.save(user);

        // Solo los artistas reciben el ArtistProfile extra — una cuenta de sello/admin
        // no tiene nada que poner ahí (ver el comentario de ArtistProfile).
        if (user.getRole() == Role.ARTIST) {
            ArtistProfile profile = new ArtistProfile();
            profile.setUser(user);
            artistProfileRepository.save(profile);
        }

        return user;
    }

    // Devuelve null si el email/contraseña no son válidos — el controller decide el 401.
    // Todos los caminos de fallo de abajo (campo vacío, email inexistente, contraseña
    // incorrecta) devuelven el mismo null a propósito: si distinguiéramos "ese email no
    // existe" de "contraseña incorrecta" se filtraría qué emails están registrados.
    public Session login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            return null;
        }

        // Un login exitoso crea una sesión/token nueva en vez de reusar una vieja —
        // loguearse dos veces deja dos tokens válidos al mismo tiempo (ej. dos dispositivos).
        Session session = new Session();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS));
        return sessionRepository.save(session);
    }
}
