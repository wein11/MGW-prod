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

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User register(User incoming) {
        User user = new User();
        user.setEmail(incoming.getEmail());
        user.setPasswordHash(passwordHasher.hash(incoming.getPassword()));
        user.setPassword(incoming.getPassword());
        user.setDisplayName(incoming.getDisplayName());
        user.setRole(incoming.getRole());
        user.setCity(incoming.getCity());
        user = userRepository.save(user);

        if (user.getRole() == Role.ARTIST) {
            ArtistProfile profile = new ArtistProfile();
            profile.setUser(user);
            artistProfileRepository.save(profile);
        }

        return user;
    }

    // Devuelve null si el email o la contraseña no coinciden (el controller responde 401).
    public Session login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            return null;
        }

        Session session = new Session();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS));
        return sessionRepository.save(session);
    }
}
