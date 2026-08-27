# mgw-prod `users` Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `users` module end to end for Etapa 1 — registration, login, and profile view/edit — as a self-contained vertical slice (Controller→Service→Repository→Entity) that `catalog`, `orders`, and `challenges` can depend on for authentication.

**Architecture:** Spring Data JPA entities + `JpaRepository` interfaces, a `Service` layer holding all business rules, `@RestController`s exposing the endpoints from the design spec, and a homemade session-token auth (no Spring Security) enforced by a `HandlerInterceptor`. Errors flow through a shared `@RestControllerAdvice` in a new `common` package.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Data JPA, Lombok, JUnit 5 + Mockito (`spring-boot-starter-webmvc-test`) for tests.

## Global Constraints

- **Prerequisite:** the `2026-08-27-mgw-prod-bootstrap.md` plan must be done first (running app, MySQL connected).
- Package root: `com.mgwprod`. This module lives under `com.mgwprod.users` (plus a small shared `com.mgwprod.common` package for cross-module error handling).
- Password hashing: **SHA-256 + random salt, using only `java.security.MessageDigest`/`SecureRandom` from the JDK** — no BCrypt, no `spring-security-crypto`, no Spring Security. This deviates from the design spec's casual mention of "BCrypt" as an example; the actual implementation is JDK-only so we don't add a dependency beyond the cátedra's own skeleton. Functionally equivalent for the TPO's purposes (salted, one-way hash, verifiable).
- Spring Boot 4.1.0 uses `@MockitoBean` (package `org.springframework.test.context.bean.override.mockito.MockitoBean`) for mocking beans in slice tests — **not** the older, deprecated `@MockBean`. If `mvn test` fails to resolve this import, that's a signal the actual Spring Boot version in `pom.xml` differs from 4.1.0; check with whoever set up bootstrap.
- Auth is stateless-token based: `Authorization: Bearer <token>` header, validated against a `Session` table. The interceptor **does not** reject requests with no `Authorization` header — it only rejects a header that's present but invalid/expired. Whether an endpoint requires auth at all is each controller's own decision (checked via the `userId` request attribute being null or not).
- Every entity relationship crosses modules only via `User` FK (per the design spec's module dependency rule) — `catalog`/`orders`/`challenges` will store a `producerId`/`artistId` `Long` column pointing at `users`, never the other way around.
- **Schema is manual, not Hibernate auto-DDL:** `application.properties` sets `spring.jpa.hibernate.ddl-auto=none` (changed 2026-08-27 to match Clase 4's "Arquitectura Spring" — the cátedra teaches hand-written schema, not Spring auto-creating/modifying tables). Every task that introduces a new `@Entity` must add its `CREATE TABLE` to `docs/db/schema.sql` (one shared, growing file — later tasks append, they don't replace earlier statements) and run it against the local database before any manual verification. The plan's automated tests (Mockito/`@WebMvcTest`) never touch a real database, so this only matters for `mvn spring-boot:run` and the manual curl/Postman checks.
- All commands assume working directory `projects/mgw-prod/`. Create a new branch (e.g. `feature/etapa1-users-module`) from `main` — the bootstrap plan's branch was already merged and deleted.

---

### Task 1: User registration (`POST /api/auth/register`)

**Files:**
- Create: `src/main/java/com/mgwprod/common/exception/ApiException.java`
- Create: `src/main/java/com/mgwprod/common/dto/ErrorResponse.java`
- Create: `src/main/java/com/mgwprod/common/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/mgwprod/users/model/Role.java`
- Create: `src/main/java/com/mgwprod/users/model/User.java`
- Create: `src/main/java/com/mgwprod/users/model/ProducerProfile.java`
- Create: `src/main/java/com/mgwprod/users/model/ArtistProfile.java`
- Create: `src/main/java/com/mgwprod/users/repository/UserRepository.java`
- Create: `src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java`
- Create: `src/main/java/com/mgwprod/users/repository/ArtistProfileRepository.java`
- Create: `src/main/java/com/mgwprod/users/security/PasswordHasher.java`
- Create: `src/main/java/com/mgwprod/users/dto/RegisterRequest.java`
- Create: `src/main/java/com/mgwprod/users/dto/UserResponse.java`
- Create: `src/main/java/com/mgwprod/users/dto/ProducerProfileDto.java`
- Create: `src/main/java/com/mgwprod/users/dto/ArtistProfileDto.java`
- Create: `src/main/java/com/mgwprod/users/exception/EmailAlreadyExistsException.java`
- Create: `src/main/java/com/mgwprod/users/service/AuthService.java`
- Create: `src/main/java/com/mgwprod/users/controller/AuthController.java`
- Test: `src/test/java/com/mgwprod/users/security/PasswordHasherTest.java`
- Test: `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`
- Test: `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`

**Interfaces:**
- Consumes: nothing (first task).
- Produces: `User` entity (`id: Long, email: String, passwordHash: String, displayName: String, role: Role, city: String, isAdmin: boolean, createdAt: Instant`), `Role` enum (`PRODUCER`, `ARTIST`), `PasswordHasher` (`hash(String): String`, `matches(String, String): boolean`), `ApiException` (abstract base every module's exceptions should extend so `GlobalExceptionHandler` catches them), `ErrorResponse` (`status: int, message: String, timestamp: Instant`).

- [ ] **Step 1: Write the failing test for `PasswordHasher`**

`src/test/java/com/mgwprod/users/security/PasswordHasherTest.java`:

```java
package com.mgwprod.users.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = passwordHasher.hash("supersecret123");
        assertTrue(passwordHasher.matches("supersecret123", hash));
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = passwordHasher.hash("supersecret123");
        assertFalse(passwordHasher.matches("wrongpassword", hash));
    }

    @Test
    void hashProducesDifferentOutputForSamePasswordDueToRandomSalt() {
        String hash1 = passwordHasher.hash("supersecret123");
        String hash2 = passwordHasher.hash("supersecret123");
        assertNotEquals(hash1, hash2);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn test -Dtest=PasswordHasherTest
```

Expected: FAIL to compile — `PasswordHasher` doesn't exist yet.

- [ ] **Step 3: Implement `PasswordHasher`**

`src/main/java/com/mgwprod/users/security/PasswordHasher.java`:

```java
package com.mgwprod.users.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {

    private static final int SALT_LENGTH_BYTES = 16;

    public String hash(String rawPassword) {
        byte[] salt = generateSalt();
        byte[] hash = digest(rawPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String storedHash) {
        String[] parts = storedHash.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = digest(rawPassword, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private byte[] digest(String rawPassword, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(rawPassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
```

- [ ] **Step 4: Run it to verify it passes**

```bash
mvn test -Dtest=PasswordHasherTest
```

Expected: PASS, 3 tests green.

- [ ] **Step 5: Create the shared error-handling infra**

`src/main/java/com/mgwprod/common/exception/ApiException.java`:

```java
package com.mgwprod.common.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

`src/main/java/com/mgwprod/common/dto/ErrorResponse.java`:

```java
package com.mgwprod.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final int status;
    private final String message;
    private final Instant timestamp;

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now());
    }
}
```

`src/main/java/com/mgwprod/common/exception/GlobalExceptionHandler.java`:

```java
package com.mgwprod.common.exception;

import com.mgwprod.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
    }
}
```

- [ ] **Step 6: Create the domain model**

`src/main/java/com/mgwprod/users/model/Role.java`:

```java
package com.mgwprod.users.model;

public enum Role {
    PRODUCER,
    ARTIST
}
```

`src/main/java/com/mgwprod/users/model/User.java`:

```java
package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

`src/main/java/com/mgwprod/users/model/ProducerProfile.java`:

```java
package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProducerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String genres;

    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Column(name = "experience_level")
    private String experienceLevel;
}
```

Note: `musicScore` is deliberately **not** a column here — per the design spec it's computed from `challenges` module data (average `Vote.score` per `Submission`, summed across a producer's submissions), not stored on the profile.

`src/main/java/com/mgwprod/users/model/ArtistProfile.java`:

```java
package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String genres;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
```

- [ ] **Step 7: Create the repositories**

`src/main/java/com/mgwprod/users/repository/UserRepository.java`:

```java
package com.mgwprod.users.repository;

import com.mgwprod.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

`src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java`:

```java
package com.mgwprod.users.repository;

import com.mgwprod.users.model.ProducerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProducerProfileRepository extends JpaRepository<ProducerProfile, Long> {
    Optional<ProducerProfile> findByUserId(Long userId);
}
```

`src/main/java/com/mgwprod/users/repository/ArtistProfileRepository.java`:

```java
package com.mgwprod.users.repository;

import com.mgwprod.users.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    Optional<ArtistProfile> findByUserId(Long userId);
}
```

- [ ] **Step 8: Create the DTOs and the registration exception**

`src/main/java/com/mgwprod/users/dto/RegisterRequest.java`:

```java
package com.mgwprod.users.dto;

import com.mgwprod.users.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre a mostrar es obligatorio")
    private String displayName;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    private String city;
}
```

`src/main/java/com/mgwprod/users/dto/ProducerProfileDto.java`:

```java
package com.mgwprod.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProducerProfileDto {
    private final String genres;
    private final Integer bpmMin;
    private final Integer bpmMax;
    private final String experienceLevel;
}
```

`src/main/java/com/mgwprod/users/dto/ArtistProfileDto.java`:

```java
package com.mgwprod.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArtistProfileDto {
    private final String genres;
    private final String bio;
}
```

`src/main/java/com/mgwprod/users/dto/UserResponse.java`:

```java
package com.mgwprod.users.dto;

import com.mgwprod.users.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserResponse {
    private final Long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final String city;
    private final boolean isAdmin;
    private final Instant createdAt;
    private final ProducerProfileDto producerProfile;
    private final ArtistProfileDto artistProfile;
}
```

`src/main/java/com/mgwprod/users/exception/EmailAlreadyExistsException.java`:

```java
package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "Ya existe un usuario con el email: " + email);
    }
}
```

- [ ] **Step 9: Write the failing test for `AuthService.register`**

`src/test/java/com/mgwprod/users/service/AuthServiceTest.java`:

```java
package com.mgwprod.users.service;

import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import com.mgwprod.users.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

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
    private SessionRepositoryPlaceholder unused; // removed once SessionRepository exists (Task 2)

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, producerProfileRepository,
                artistProfileRepository, new PasswordHasher());
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
}
```

Ignore the `SessionRepositoryPlaceholder` mock — delete that line now; it was a placeholder for Task 2's dependency and does not belong in this task. The final file for this step has only the three `@Mock` fields shown (`userRepository`, `producerProfileRepository`, `artistProfileRepository`) and the constructor call exactly as `new AuthService(userRepository, producerProfileRepository, artistProfileRepository, new PasswordHasher())`.

- [ ] **Step 10: Run it to verify it fails**

```bash
mvn test -Dtest=AuthServiceTest
```

Expected: FAIL to compile — `AuthService` doesn't exist yet.

- [ ] **Step 11: Implement `AuthService.register`**

`src/main/java/com/mgwprod/users/service/AuthService.java`:

```java
package com.mgwprod.users.service;

import com.mgwprod.users.dto.ArtistProfileDto;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import com.mgwprod.users.security.PasswordHasher;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProducerProfileRepository producerProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository,
                        ProducerProfileRepository producerProfileRepository,
                        ArtistProfileRepository artistProfileRepository,
                        PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.producerProfileRepository = producerProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.passwordHasher = passwordHasher;
    }

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
}
```

- [ ] **Step 12: Run it to verify it passes**

```bash
mvn test -Dtest=AuthServiceTest
```

Expected: PASS, 2 tests green.

- [ ] **Step 13: Write the failing controller test**

`src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`:

```java
package com.mgwprod.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithUserData() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.PRODUCER);

        UserResponse response = new UserResponse(1L, "productor@test.com", "DJ Test",
                Role.PRODUCER, null, false, Instant.now(),
                new ProducerProfileDto(null, null, null, null), null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("productor@test.com"))
                .andExpect(jsonPath("$.role").value("PRODUCER"));
    }

    @Test
    void registerReturns400WhenEmailIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.PRODUCER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicado@test.com");
        request.setPassword("supersecret123");
        request.setDisplayName("DJ Test");
        request.setRole(Role.ARTIST);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("duplicado@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 14: Run it to verify it fails**

```bash
mvn test -Dtest=AuthControllerTest
```

Expected: FAIL to compile — `AuthController` doesn't exist yet.

- [ ] **Step 15: Implement `AuthController.register`**

`src/main/java/com/mgwprod/users/controller/AuthController.java`:

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.dto.RegisterRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

- [ ] **Step 16: Run it to verify it passes**

```bash
mvn test -Dtest=AuthControllerTest
```

Expected: PASS, 3 tests green.

- [ ] **Step 17: Create the SQL schema for this task's entities**

Since `ddl-auto=none` (see Global Constraints), Hibernate won't create these tables — you have
to. Create `docs/db/schema.sql` at the project root with:

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    city VARCHAR(255),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL
);

CREATE TABLE producer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    genres VARCHAR(255),
    bpm_min INT,
    bpm_max INT,
    experience_level VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE artist_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    genres VARCHAR(255),
    bio TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Run it against your local database:

```bash
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

- [ ] **Step 18: Manual smoke test against the real database**

The automated tests (Mockito/`@WebMvcTest`) never touch MySQL, so this is the first real proof
the entity mappings and the schema actually agree. Start the app and register a real user:

```bash
mvn spring-boot:run
```

In another terminal:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"productor@test.com","password":"supersecret123","displayName":"DJ Test","role":"PRODUCER"}'
```

Expected: `201 Created` with a JSON body containing the new user's `id`, `email`, `role`, and a
`producerProfile` object. If you get a SQL error instead (e.g. "Table 'mgw_prod.users' doesn't
exist"), Step 17's script didn't run against the right database — check
`spring.datasource.url` in `application.properties` matches the database you ran it against.
Stop the app (Ctrl+C) once confirmed.

- [ ] **Step 19: Run the full suite and commit**

```bash
mvn test
git add src/main/java/com/mgwprod/common src/main/java/com/mgwprod/users src/test/java/com/mgwprod/users docs/db/schema.sql
git commit -m "feat(users): add registration endpoint with role-specific profile creation"
```

---

### Task 2: Login (`POST /api/auth/login`)

**Files:**
- Create: `src/main/java/com/mgwprod/users/model/Session.java`
- Create: `src/main/java/com/mgwprod/users/repository/SessionRepository.java`
- Create: `src/main/java/com/mgwprod/users/dto/LoginRequest.java`
- Create: `src/main/java/com/mgwprod/users/dto/LoginResponse.java`
- Create: `src/main/java/com/mgwprod/users/exception/InvalidCredentialsException.java`
- Modify: `src/main/java/com/mgwprod/users/service/AuthService.java` (add `login`)
- Modify: `src/main/java/com/mgwprod/users/controller/AuthController.java` (add `POST /login`)
- Modify: `src/test/java/com/mgwprod/users/service/AuthServiceTest.java` (add login tests, fix constructor call)
- Modify: `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java` (add login tests)

**Interfaces:**
- Consumes: `User` entity, `PasswordHasher.matches(String, String): boolean` (Task 1).
- Produces: `Session` entity (`id, user: User, token: String, expiresAt: Instant`), `SessionRepository.findByToken(String): Optional<Session>` — this is what Task 4's auth interceptor consumes.

- [ ] **Step 1: Create the `Session` entity**

`src/main/java/com/mgwprod/users/model/Session.java`:

```java
package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
```

- [ ] **Step 2: Create `SessionRepository`**

`src/main/java/com/mgwprod/users/repository/SessionRepository.java`:

```java
package com.mgwprod.users.repository;

import com.mgwprod.users.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
}
```

- [ ] **Step 3: Create the login DTOs and exception**

`src/main/java/com/mgwprod/users/dto/LoginRequest.java`:

```java
package com.mgwprod.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
```

`src/main/java/com/mgwprod/users/dto/LoginResponse.java`:

```java
package com.mgwprod.users.dto;

import com.mgwprod.users.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String token;
    private final Long userId;
    private final String displayName;
    private final Role role;
}
```

`src/main/java/com/mgwprod/users/exception/InvalidCredentialsException.java`:

```java
package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
    }
}
```

- [ ] **Step 4: Update `AuthServiceTest` — add the `SessionRepository` mock and login tests**

Replace the mock fields and constructor call in `src/test/java/com/mgwprod/users/service/AuthServiceTest.java` with:

```java
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
```

Then add these test methods to the same class (and the matching imports: `com.mgwprod.users.repository.SessionRepository`, `com.mgwprod.users.dto.LoginRequest`, `com.mgwprod.users.dto.LoginResponse`, `com.mgwprod.users.exception.InvalidCredentialsException`, `java.util.Optional`):

```java
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
```

- [ ] **Step 5: Run to verify it fails**

```bash
mvn test -Dtest=AuthServiceTest
```

Expected: FAIL to compile — `AuthService` constructor doesn't take a `SessionRepository` yet, and `login` doesn't exist.

- [ ] **Step 6: Add `login` to `AuthService`**

Update `src/main/java/com/mgwprod/users/service/AuthService.java` to the full new content:

```java
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
```

- [ ] **Step 7: Run to verify it passes**

```bash
mvn test -Dtest=AuthServiceTest
```

Expected: PASS, 5 tests green (2 register + 3 login).

- [ ] **Step 8: Write the failing controller test for login**

Add to `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java` (imports: `com.mgwprod.users.dto.LoginRequest`, `com.mgwprod.users.dto.LoginResponse`, `com.mgwprod.users.exception.InvalidCredentialsException`):

```java
    @Test
    void loginReturns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("supersecret123");

        LoginResponse response = new LoginResponse("some-token-123", 1L, "DJ Test", Role.PRODUCER);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("some-token-123"));
    }

    @Test
    void loginReturns401WithWrongCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("productor@test.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 9: Run to verify it fails**

```bash
mvn test -Dtest=AuthControllerTest
```

Expected: FAIL to compile — `AuthController` has no `/login` mapping yet.

- [ ] **Step 10: Add the login endpoint to `AuthController`**

Add this method inside `src/main/java/com/mgwprod/users/controller/AuthController.java` (and import `com.mgwprod.users.dto.LoginRequest`, `com.mgwprod.users.dto.LoginResponse`):

```java
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
```

- [ ] **Step 11: Run to verify it passes**

```bash
mvn test -Dtest=AuthControllerTest
```

Expected: PASS, 5 tests green (3 register + 2 login).

- [ ] **Step 12: Append the `sessions` table to the SQL schema**

Add this to the end of `docs/db/schema.sql` (don't recreate the earlier tables — this appends):

```sql
CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Run just the new statement against your local database:

```bash
mysql -u root -padmin mgw_prod <<'EOF'
CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
EOF
```

- [ ] **Step 13: Manual smoke test — login against the real database**

```bash
mvn spring-boot:run
```

In another terminal (reusing the user registered in Task 1's smoke test):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"productor@test.com","password":"supersecret123"}'
```

Expected: `200 OK` with a JSON body containing a `token`. Stop the app (Ctrl+C) once confirmed.

- [ ] **Step 14: Run the full suite and commit**

```bash
mvn test
git add src/main/java/com/mgwprod/users src/test/java/com/mgwprod/users docs/db/schema.sql
git commit -m "feat(users): add login endpoint with session token issuance"
```

---

### Task 3: Public profile view (`GET /api/users/{id}`)

**Files:**
- Create: `src/main/java/com/mgwprod/users/service/UserService.java`
- Create: `src/main/java/com/mgwprod/users/controller/UserController.java`
- Create: `src/main/java/com/mgwprod/users/exception/UserNotFoundException.java`
- Test: `src/test/java/com/mgwprod/users/service/UserServiceTest.java`
- Test: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`

**Interfaces:**
- Consumes: `User`, `ProducerProfile`, `ArtistProfile` entities and their repositories (Task 1).
- Produces: `UserService.getById(Long): UserResponse` — reused and extended by Task 4's `update`.

- [ ] **Step 1: Create `UserNotFoundException`**

`src/main/java/com/mgwprod/users/exception/UserNotFoundException.java`:

```java
package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "No existe un usuario con id: " + userId);
    }
}
```

- [ ] **Step 2: Write the failing test for `UserService.getById`**

`src/test/java/com/mgwprod/users/service/UserServiceTest.java`:

```java
package com.mgwprod.users.service;

import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProducerProfileRepository producerProfileRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, producerProfileRepository, artistProfileRepository);
    }

    @Test
    void getByIdReturnsUserWithProducerProfile() {
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("RKT,Trap");
        profile.setBpmMin(90);
        profile.setBpmMax(140);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserResponse response = userService.getById(1L);

        assertEquals("productor@test.com", response.getEmail());
        assertEquals("RKT,Trap", response.getProducerProfile().getGenres());
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }
}
```

- [ ] **Step 3: Run to verify it fails**

```bash
mvn test -Dtest=UserServiceTest
```

Expected: FAIL to compile — `UserService` doesn't exist yet.

- [ ] **Step 4: Implement `UserService`**

`src/main/java/com/mgwprod/users/service/UserService.java`:

```java
package com.mgwprod.users.service;

import com.mgwprod.users.dto.ArtistProfileDto;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProducerProfileRepository producerProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public UserService(UserRepository userRepository,
                        ProducerProfileRepository producerProfileRepository,
                        ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.producerProfileRepository = producerProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        ProducerProfileDto producerDto = null;
        ArtistProfileDto artistDto = null;

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = producerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + user.getId()));
            producerDto = new ProducerProfileDto(profile.getGenres(), profile.getBpmMin(),
                    profile.getBpmMax(), profile.getExperienceLevel());
        } else {
            ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + user.getId()));
            artistDto = new ArtistProfileDto(profile.getGenres(), profile.getBio());
        }

        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getCity(), user.isAdmin(), user.getCreatedAt(),
                producerDto, artistDto);
    }
}
```

- [ ] **Step 5: Run to verify it passes**

```bash
mvn test -Dtest=UserServiceTest
```

Expected: PASS, 2 tests green.

- [ ] **Step 6: Write the failing controller test**

`src/test/java/com/mgwprod/users/controller/UserControllerTest.java`:

```java
package com.mgwprod.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
```

- [ ] **Step 7: Run to verify it fails**

```bash
mvn test -Dtest=UserControllerTest
```

Expected: FAIL to compile — `UserController` doesn't exist yet.

- [ ] **Step 8: Implement `UserController`**

`src/main/java/com/mgwprod/users/controller/UserController.java`:

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
}
```

- [ ] **Step 9: Run to verify it passes**

```bash
mvn test -Dtest=UserControllerTest
```

Expected: PASS, 2 tests green.

- [ ] **Step 10: Run the full suite and commit**

```bash
mvn test
git add src/main/java/com/mgwprod/users src/test/java/com/mgwprod/users
git commit -m "feat(users): add public user profile endpoint"
```

---

### Task 4: Session auth interceptor + profile update (`PUT /api/users/{id}`)

**Files:**
- Create: `src/main/java/com/mgwprod/users/security/SessionAuthInterceptor.java`
- Create: `src/main/java/com/mgwprod/users/config/WebConfig.java`
- Create: `src/main/java/com/mgwprod/users/dto/UpdateUserRequest.java`
- Create: `src/main/java/com/mgwprod/users/exception/ForbiddenOperationException.java`
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java` (add `update`)
- Modify: `src/main/java/com/mgwprod/users/controller/UserController.java` (add `PUT /{id}`)
- Test: `src/test/java/com/mgwprod/users/security/SessionAuthInterceptorTest.java`
- Modify: `src/test/java/com/mgwprod/users/service/UserServiceTest.java` (add update tests)
- Modify: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java` (add update tests)

**Interfaces:**
- Consumes: `SessionRepository.findByToken` (Task 2), `UserService.getById` internals (Task 3).
- Produces: `request.getAttribute("userId"): Long` and `request.getAttribute("userRole"): String` — **this is what `catalog`, `orders`, and `challenges` controllers read to know who's authenticated**, once this interceptor is registered globally. It never rejects a request with no `Authorization` header; each protected controller must itself check whether `userId` came back null and respond accordingly (see `updateUser` below for the pattern to copy).

- [ ] **Step 1: Write the failing test for the interceptor**

`src/test/java/com/mgwprod/users/security/SessionAuthInterceptorTest.java`:

```java
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
        user.setRole(Role.PRODUCER);

        Session session = new Session();
        session.setUser(user);
        session.setToken("valid-token");
        session.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(sessionRepository.findByToken("valid-token")).thenReturn(Optional.of(session));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(request).setAttribute(SessionAuthInterceptor.USER_ID_ATTRIBUTE, 42L);
        verify(request).setAttribute(SessionAuthInterceptor.USER_ROLE_ATTRIBUTE, "PRODUCER");
    }

    @Test
    void rejectsExpiredToken() {
        User user = new User();
        user.setId(42L);
        user.setRole(Role.PRODUCER);

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
```

- [ ] **Step 2: Run to verify it fails**

```bash
mvn test -Dtest=SessionAuthInterceptorTest
```

Expected: FAIL to compile — `SessionAuthInterceptor` doesn't exist yet.

- [ ] **Step 3: Implement `SessionAuthInterceptor`**

`src/main/java/com/mgwprod/users/security/SessionAuthInterceptor.java`:

```java
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
```

- [ ] **Step 4: Run to verify it passes**

```bash
mvn test -Dtest=SessionAuthInterceptorTest
```

Expected: PASS, 3 tests green.

- [ ] **Step 5: Register the interceptor**

`src/main/java/com/mgwprod/users/config/WebConfig.java`:

```java
package com.mgwprod.users.config;

import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.security.SessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionRepository sessionRepository;

    public WebConfig(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionAuthInterceptor(sessionRepository))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
```

- [ ] **Step 6: Create `ForbiddenOperationException` and `UpdateUserRequest`**

`src/main/java/com/mgwprod/users/exception/ForbiddenOperationException.java`:

```java
package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
```

`src/main/java/com/mgwprod/users/dto/UpdateUserRequest.java`:

```java
package com.mgwprod.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String displayName;
    private String city;
    private String genres;
    private Integer bpmMin;
    private Integer bpmMax;
    private String experienceLevel;
    private String bio;
}
```

- [ ] **Step 7: Write the failing service test for `update`**

Add to `src/test/java/com/mgwprod/users/service/UserServiceTest.java` (imports: `com.mgwprod.users.dto.UpdateUserRequest`, `com.mgwprod.users.exception.ForbiddenOperationException`):

```java
    @Test
    void updateChangesDisplayNameForOwner() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("Old Name");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        ProducerProfile profile = new ProducerProfile();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("New Name");

        UserResponse response = userService.update(1L, 1L, request);

        assertEquals("New Name", response.getDisplayName());
    }

    @Test
    void updateThrowsForbiddenWhenEditingSomeoneElse() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("New Name");

        assertThrows(ForbiddenOperationException.class, () -> userService.update(1L, 2L, request));
    }
```

- [ ] **Step 8: Run to verify it fails**

```bash
mvn test -Dtest=UserServiceTest
```

Expected: FAIL to compile — `UserService.update` doesn't exist yet.

- [ ] **Step 9: Add `update` to `UserService`**

Add this method to `src/main/java/com/mgwprod/users/service/UserService.java` (and import `com.mgwprod.users.dto.UpdateUserRequest`, `com.mgwprod.users.exception.ForbiddenOperationException`):

```java
    public UserResponse update(Long targetUserId, Long requestingUserId, UpdateUserRequest request) {
        if (!targetUserId.equals(requestingUserId)) {
            throw new ForbiddenOperationException("No podés editar el perfil de otro usuario");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        userRepository.save(user);

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = producerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + user.getId()));
            if (request.getGenres() != null) {
                profile.setGenres(request.getGenres());
            }
            if (request.getBpmMin() != null) {
                profile.setBpmMin(request.getBpmMin());
            }
            if (request.getBpmMax() != null) {
                profile.setBpmMax(request.getBpmMax());
            }
            if (request.getExperienceLevel() != null) {
                profile.setExperienceLevel(request.getExperienceLevel());
            }
            producerProfileRepository.save(profile);
        } else {
            ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + user.getId()));
            if (request.getGenres() != null) {
                profile.setGenres(request.getGenres());
            }
            if (request.getBio() != null) {
                profile.setBio(request.getBio());
            }
            artistProfileRepository.save(profile);
        }

        return toResponse(user);
    }
```

- [ ] **Step 10: Run to verify it passes**

```bash
mvn test -Dtest=UserServiceTest
```

Expected: PASS, 4 tests green.

- [ ] **Step 11: Write the failing controller tests for update**

Add to `src/test/java/com/mgwprod/users/controller/UserControllerTest.java` (imports: `com.mgwprod.users.dto.UpdateUserRequest`, `org.springframework.http.MediaType`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.ArgumentMatchers.eq`, `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`):

```java
    @Test
    void updateUserReturns200WhenOwnerEditsOwnProfile() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Nuevo Nombre");

        UserResponse response = new UserResponse(1L, "productor@test.com", "Nuevo Nombre",
                Role.PRODUCER, null, false, Instant.now(),
                new ProducerProfileDto(null, null, null, null), null);

        when(userService.update(eq(1L), eq(1L), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nuevo Nombre"));
    }

    @Test
    void updateUserReturns403WhenNoUserIdAttribute() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Nuevo Nombre");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 12: Run to verify it fails**

```bash
mvn test -Dtest=UserControllerTest
```

Expected: FAIL to compile — `UserController` has no `PUT /{id}` mapping yet.

- [ ] **Step 13: Add the update endpoint to `UserController`**

Add this method to `src/main/java/com/mgwprod/users/controller/UserController.java` (and import `com.mgwprod.users.dto.UpdateUserRequest`, `com.mgwprod.users.exception.ForbiddenOperationException`, `org.springframework.web.bind.annotation.PutMapping`, `org.springframework.web.bind.annotation.RequestAttribute`, `org.springframework.web.bind.annotation.RequestBody`):

```java
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                    @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                    @RequestBody UpdateUserRequest request) {
        if (requestingUserId == null) {
            throw new ForbiddenOperationException("Necesitás iniciar sesión para editar un perfil");
        }
        return userService.update(id, requestingUserId, request);
    }
```

- [ ] **Step 14: Run to verify it passes**

```bash
mvn test -Dtest=UserControllerTest
```

Expected: PASS, 4 tests green.

- [ ] **Step 15: Run the full suite, verify the app still boots, and commit**

```bash
mvn test
mvn spring-boot:run
```

Manually verify with curl (app must be running, and you must have registered + logged in a user first via `/api/auth/register` and `/api/auth/login`):

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <token-from-login-response>" \
  -H "Content-Type: application/json" \
  -d '{"displayName": "Nuevo Nombre"}'
```

Expected: `200 OK` with the updated `displayName` in the JSON body. Stop the app (Ctrl+C), then:

```bash
git add src/main/java/com/mgwprod/users src/test/java/com/mgwprod/users
git commit -m "feat(users): add session auth interceptor and protected profile update"
```

---

## Self-Review

**Spec coverage:** All `users` module endpoints from the design spec are covered — `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/{id}`, `PUT /api/users/{id}`. `Session`, `User`, `ProducerProfile`, `ArtistProfile` entities match the spec's domain model, including the deliberate omission of a stored `musicScore` column (documented in Task 1, Step 6). The spec's "flag admin simple en User" (`isAdmin`) is modeled on `User` but has no endpoint yet in this plan — creating challenges is out of scope for the `users` module and belongs to the `challenges` module plan.

**Placeholder scan:** No TBD/TODO. The one deliberately-marked placeholder (`SessionRepositoryPlaceholder` in Task 1 Step 9) is explicitly called out as something to delete, not a gap to fill in later — it exists only to make clear which mock fields belong to Task 1 versus Task 2.

**Type consistency:** `UserResponse`, `ProducerProfileDto`, `ArtistProfileDto` constructor signatures are identical everywhere they're used across Tasks 1, 3, and 4. `AuthService`'s constructor signature changes once, in Task 2 Step 6, with the corresponding test update in Task 2 Step 4 — both edited together so they never drift. `SessionAuthInterceptor.USER_ID_ATTRIBUTE`/`USER_ROLE_ATTRIBUTE` constants are defined once (Task 4) and are the single source of truth other modules should reference by name, not by re-typing the string `"userId"`.

**Post-Clase 4 update (2026-08-27):** added Task 1 Steps 17-18 and Task 2 Steps 12-13 to create/append `docs/db/schema.sql` and manually smoke-test against the real database — required once `ddl-auto` switched from `update` to `none`. Every subsequent `CREATE TABLE` in this plan appends to the same file rather than replacing it, so Task 2's schema step explicitly says not to recreate Task 1's tables.
