package com.mgwprod.users.service;

import com.mgwprod.users.dto.ArtistProfileDto;
import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
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
    private final ProducerProfileRepository producerProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository,
                        ProducerProfileRepository producerProfileRepository,
                        ArtistProfileRepository artistProfileRepository,
                        SessionRepository sessionRepository,
                        PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.producerProfileRepository = producerProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole());
        user.setCity(request.getCity());
        user = userRepository.save(user);

        ProducerProfileDto producerDto = null;
        ArtistProfileDto artistDto = null;

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = new ProducerProfile();
            profile.setUser(user);
            producerProfileRepository.save(profile);
            producerDto = new ProducerProfileDto(null, null, null, null);
        } else {
            ArtistProfile profile = new ArtistProfile();
            profile.setUser(user);
            artistProfileRepository.save(profile);
            artistDto = new ArtistProfileDto(null, null);
        }

        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getCity(), user.isAdmin(), user.getCreatedAt(),
                producerDto, artistDto);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Session session = new Session();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS));
        sessionRepository.save(session);

        return new LoginResponse(session.getToken(), user.getId(), user.getDisplayName(), user.getRole());
    }
}
