# Remove DTO Layer from com.mgwprod.users Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the `com.mgwprod.users.dto` package and expose JPA entities directly from `AuthController`/`UserController`, so the `users` module matches the literal pattern taught in Clase 4 of Aplicaciones Interactivas (controller → entity → repository → service, no DTOs).

**Architecture:** Controllers bind `@RequestBody`/`@RequestParam` straight to `User`, `Session`, `ProducerProfile`, `ArtistProfile`. Since the class demo never combined two entities in one response, `GET`/`PUT` for the role-specific profile (genres/bpm/bio) move to their own `/profile` sub-resource endpoints instead of being merged into the `User` payload. Security-sensitive fields (`passwordHash`, the plaintext `password` used only on register) are locked down with Jackson's `@JsonIgnore` / `@JsonProperty(access = ...)` so entity-direct exposure doesn't leak secrets or allow overposting — services still hand-copy fields onto persisted entities rather than saving client-supplied objects wholesale.

**Tech Stack:** Java, Spring Boot (Spring Web, Spring Data JPA, Bean Validation), Lombok, Jackson 3 (`tools.jackson`), JUnit 5, Mockito, MockMvc.

**Spec:** `facultad/aplicaciones-interactivas/clases/2026-08-27-clase-04/Backend con Spring.pdf` and `facultad/aplicaciones-interactivas/clases/2026-08-27-clase-04/webcampus/` (reference project) — both under the workspace hub root, two levels above this project.

## Global Constraints

- Every endpoint returns/accepts an `@Entity` directly — no request/response classes in `com.mgwprod.users.dto` (that package is deleted by the end of this plan).
- No DAO pattern — persistence stays on `JpaRepository` (unchanged, not touched by this plan).
- `passwordHash` must never appear in any JSON response or accept a value from any JSON request (`@JsonIgnore`).
- The plaintext `password` field used for register must never appear in a JSON response (`@JsonProperty(access = WRITE_ONLY)`).
- No new Spring/Jackson features beyond what's strictly needed to keep entity-direct exposure secure: `@Transient`, `@JsonIgnore`, `@JsonProperty(access=...)`, `@RequestParam`. Nothing else (no Spring Security, no bidirectional JPA relations, no serialization views).
- `docs/db/schema.sql` is not touched — no persisted column changes in this plan (`password` is `@Transient`).
- `catalog`, `orders`, `challenges` packages do not exist yet and are out of scope.

---

### Task 1: Harden the `User` entity for direct JSON exposure

**Files:**
- Modify: `src/main/java/com/mgwprod/users/model/User.java`
- Test: `src/test/java/com/mgwprod/users/model/UserJsonTest.java` (create)

**Interfaces:**
- Produces: `User` gains a `@Transient` `password` field (`getPassword()`/`setPassword()`, write-only in JSON) alongside the existing `passwordHash` (now `@JsonIgnore`), and the boolean field is renamed `admin` internally so its getter/setter stay `isAdmin()`/`setAdmin(boolean)` (no caller-visible change) while its JSON key stays `"isAdmin"`.

- [ ] **Step 1: Write the failing test**

```java
package com.mgwprod.users.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passwordHashIsNeverSerialized() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("some-hash");
        user.setDisplayName("Test");
        user.setRole(Role.ARTIST);

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("some-hash"));
    }

    @Test
    void plaintextPasswordIsAcceptedOnInputButNeverSerialized() throws Exception {
        String incomingJson = """
                {"email":"test@test.com","password":"supersecret123","displayName":"Test","role":"ARTIST"}
                """;

        User user = objectMapper.readValue(incomingJson, User.class);
        assertEquals("supersecret123", user.getPassword());

        String outgoingJson = objectMapper.writeValueAsString(user);
        assertFalse(outgoingJson.contains("password\""));
    }

    @Test
    void isAdminSerializesOnceAsIsAdmin() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setDisplayName("Test");
        user.setRole(Role.ARTIST);

        String json = objectMapper.writeValueAsString(user);

        assertTrue(json.contains("\"isAdmin\":false"));
        assertFalse(json.contains("\"admin\":"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=UserJsonTest test`
Expected: FAIL — compile error (no `getPassword()`/`setPassword()` on `User` yet) or assertion failures (`passwordHash` still serializes, `"admin"` key present instead of merged `"isAdmin"`).

- [ ] **Step 3: Write minimal implementation**

Replace the full contents of `src/main/java/com/mgwprod/users/model/User.java`:

```java
package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Not persisted: only carries the raw password from POST /api/auth/register into
    // AuthService, which hashes it into passwordHash. Write-only so it's accepted from
    // the request JSON but never echoed back in a response.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre a mostrar es obligatorio")
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String city;

    // Field name must match the Lombok boolean getter's implicit property name ("admin",
    // from isAdmin() -> "admin") so both merge into one Jackson property before being
    // renamed to "isAdmin" by @JsonProperty. Otherwise Jackson serializes it twice
    // (once as "admin" from the getter, once as "isAdmin" from the annotation).
    @JsonProperty("isAdmin")
    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=UserJsonTest test`
Expected: PASS (3/3)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/model/User.java src/test/java/com/mgwprod/users/model/UserJsonTest.java
git commit -m "refactor(users): harden User entity for direct JSON exposure"
```

---

### Task 2: Add merge-validation to `ProducerProfile`/`ArtistProfile` and hide their `user` back-reference

**Files:**
- Modify: `src/main/java/com/mgwprod/users/model/ProducerProfile.java`
- Modify: `src/main/java/com/mgwprod/users/model/ArtistProfile.java`
- Test: `src/test/java/com/mgwprod/users/model/ProfileJsonTest.java` (create)

**Interfaces:**
- Consumes: none (leaf models).
- Produces: `ProducerProfile`/`ArtistProfile` no longer serialize their `user` field; their editable fields carry the same "optional but non-blank-if-present" validation `UpdateUserRequest` used to enforce.

- [ ] **Step 1: Write the failing test**

```java
package com.mgwprod.users.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void producerProfileDoesNotSerializeOwningUser() throws Exception {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setDisplayName("Owner");
        owner.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setUser(owner);
        profile.setGenres("RKT");

        String json = objectMapper.writeValueAsString(profile);

        assertFalse(json.contains("\"user\""));
    }

    @Test
    void artistProfileDoesNotSerializeOwningUser() throws Exception {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setDisplayName("Owner");
        owner.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setUser(owner);
        profile.setBio("bio");

        String json = objectMapper.writeValueAsString(profile);

        assertFalse(json.contains("\"user\""));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProfileJsonTest test`
Expected: FAIL — both assertions fail because `user` currently serializes.

- [ ] **Step 3: Write minimal implementation**

Replace the full contents of `src/main/java/com/mgwprod/users/model/ProducerProfile.java`:

```java
package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(min = 1, message = "Los géneros no pueden estar vacíos")
    private String genres;

    @Min(value = 1, message = "El BPM mínimo debe ser mayor a 0")
    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Min(value = 1, message = "El BPM máximo debe ser mayor a 0")
    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Size(min = 1, message = "El nivel de experiencia no puede estar vacío")
    @Column(name = "experience_level")
    private String experienceLevel;
}
```

Replace the full contents of `src/main/java/com/mgwprod/users/model/ArtistProfile.java`:

```java
package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(min = 1, message = "Los géneros no pueden estar vacíos")
    private String genres;

    @Size(min = 1, message = "La biografía no puede estar vacía")
    @Column(columnDefinition = "TEXT")
    private String bio;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProfileJsonTest test`
Expected: PASS (2/2)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/model/ProducerProfile.java src/main/java/com/mgwprod/users/model/ArtistProfile.java src/test/java/com/mgwprod/users/model/ProfileJsonTest.java
git commit -m "refactor(users): hide owning user and add merge-validation on profile entities"
```

---

### Task 3: Register endpoint without DTOs

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/AuthService.java`
- Modify: `src/main/java/com/mgwprod/users/controller/AuthController.java`
- Modify: `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`
- Modify: `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`

**Interfaces:**
- Consumes: `User` (Task 1), `ProducerProfile`/`ArtistProfile` (Task 2).
- Produces: `AuthService.register(User incoming): User` — later tasks (4) don't depend on this signature, but `login` in the same file must not be broken meanwhile, so this task only touches `register` and its tests.

- [ ] **Step 1: Write the failing tests**

In `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`, replace the two register tests (keep the login tests/imports as-is for now — they still reference `LoginRequest`/`LoginResponse` and will keep compiling until Task 4):

```java
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
```

Remove the now-unused `import com.mgwprod.users.dto.RegisterRequest;`, `import com.mgwprod.users.dto.UserResponse;`, and `import java.time.Instant;` lines from that file (the rewritten register test no longer calls `Instant.now()`; the login tests below still need `LoginRequest`/`LoginResponse`, keep those two imports).

In `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`, replace the three register tests and drop the `ObjectMapper`-based JSON building for them (Jackson's `WRITE_ONLY` on `User.password` means `objectMapper.writeValueAsString(...)` would silently drop the password — build the JSON by hand instead):

```java
    @Test
    void registerReturns201WithUserData() throws Exception {
        User response = new User();
        response.setId(1L);
        response.setEmail("productor@test.com");
        response.setDisplayName("DJ Test");
        response.setRole(Role.PRODUCER);
        response.setCreatedAt(Instant.now());

        when(authService.register(any(User.class))).thenReturn(response);

        String requestJson = """
                {"email":"productor@test.com","password":"supersecret123","displayName":"DJ Test","role":"PRODUCER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("productor@test.com"))
                .andExpect(jsonPath("$.role").value("PRODUCER"))
                .andExpect(jsonPath("$.isAdmin").value(false))
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerReturns400WhenEmailIsBlank() throws Exception {
        String requestJson = """
                {"email":"","password":"supersecret123","displayName":"DJ Test","role":"PRODUCER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(User.class)))
                .thenThrow(new EmailAlreadyExistsException("duplicado@test.com"));

        String requestJson = """
                {"email":"duplicado@test.com","password":"supersecret123","displayName":"DJ Test","role":"ARTIST"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
    }
```

Update that file's imports: drop `com.mgwprod.users.dto.ProducerProfileDto`, `com.mgwprod.users.dto.RegisterRequest`, `com.mgwprod.users.dto.UserResponse`; add `import com.mgwprod.users.model.User;` (the login tests below still need `LoginRequest`/`LoginResponse`, keep those two imports until Task 4).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=AuthServiceTest,AuthControllerTest test`
Expected: FAIL — compile error, `AuthService.register` still takes `RegisterRequest` and `AuthController` still routes through the DTOs.

- [ ] **Step 3: Write minimal implementation**

In `src/main/java/com/mgwprod/users/service/AuthService.java`, replace the `register` method and its imports (leave `login` untouched for now — its signature still matches the old `LoginRequest`/`LoginResponse`, Task 4 rewrites it):

```java
package com.mgwprod.users.service;

import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
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
    public User register(User incoming) {
        if (userRepository.existsByEmail(incoming.getEmail())) {
            throw new EmailAlreadyExistsException(incoming.getEmail());
        }

        User user = new User();
        user.setEmail(incoming.getEmail());
        user.setPasswordHash(passwordHasher.hash(incoming.getPassword()));
        user.setDisplayName(incoming.getDisplayName());
        user.setRole(incoming.getRole());
        user.setCity(incoming.getCity());
        user = userRepository.save(user);

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = new ProducerProfile();
            profile.setUser(user);
            producerProfileRepository.save(profile);
        } else {
            ArtistProfile profile = new ArtistProfile();
            profile.setUser(user);
            artistProfileRepository.save(profile);
        }

        return user;
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

In `src/main/java/com/mgwprod/users/controller/AuthController.java`, replace the `register` method and adjust imports (leave `login` wired to `LoginRequest`/`LoginResponse` for now):

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.dto.LoginRequest;
import com.mgwprod.users.dto.LoginResponse;
import com.mgwprod.users.model.User;
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
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User created = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=AuthServiceTest,AuthControllerTest test`
Expected: PASS — all register tests green, all login tests still green (untouched).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/service/AuthService.java src/main/java/com/mgwprod/users/controller/AuthController.java src/test/java/com/mgwprod/users/service/AuthServiceTest.java src/test/java/com/mgwprod/users/controller/AuthControllerTest.java
git commit -m "refactor(users): bind register endpoint directly to User entity"
```

---

### Task 4: Login endpoint without DTOs

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/AuthService.java`
- Modify: `src/main/java/com/mgwprod/users/controller/AuthController.java`
- Modify: `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`
- Modify: `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`
- Delete: `src/main/java/com/mgwprod/users/dto/LoginRequest.java`
- Delete: `src/main/java/com/mgwprod/users/dto/LoginResponse.java`

**Interfaces:**
- Produces: `AuthService.login(String email, String password): Session`. `Session` (existing entity, Task-independent) carries `token`, nested `user` (passwordHash hidden per Task 1), `expiresAt`.

- [ ] **Step 1: Write the failing tests**

In `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`, replace the four login tests and add a blank-credentials guard test:

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
```

Update that file's imports: drop `com.mgwprod.users.dto.LoginRequest` and `com.mgwprod.users.dto.LoginResponse`; add `import com.mgwprod.users.model.Session;` and `import java.util.Optional;` (already present from register tests if Task 3 left it — verify).

In `src/test/java/com/mgwprod/users/controller/AuthControllerTest.java`, replace the two login tests, switch to `.param(...)` calls (no JSON body — login now binds `@RequestParam`), and drop the now-unused `ObjectMapper` field/import (nothing in this file needs it anymore):

```java
    @Test
    void loginReturns200WithToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);

        Session session = new Session();
        session.setToken("some-token-123");
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        when(authService.login(eq("productor@test.com"), eq("supersecret123"))).thenReturn(session);

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "productor@test.com")
                        .param("password", "supersecret123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("some-token-123"));
    }

    @Test
    void loginReturns401WithWrongCredentials() throws Exception {
        when(authService.login(eq("productor@test.com"), eq("wrongpassword")))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "productor@test.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized());
    }
```

Update that file's imports: drop `tools.jackson.databind.ObjectMapper`, `com.mgwprod.users.dto.LoginRequest`, `com.mgwprod.users.dto.LoginResponse`; add `import com.mgwprod.users.model.Session;` and `import static org.mockito.ArgumentMatchers.eq;`; remove the `@Autowired private ObjectMapper objectMapper;` field.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=AuthServiceTest,AuthControllerTest test`
Expected: FAIL — compile error, `AuthService.login` still takes `LoginRequest`.

- [ ] **Step 3: Write minimal implementation**

In `src/main/java/com/mgwprod/users/service/AuthService.java`, replace the `login` method and drop the now-unused `LoginRequest`/`LoginResponse` imports:

```java
    public Session login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Session session = new Session();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS));
        return sessionRepository.save(session);
    }
```

In `src/main/java/com/mgwprod/users/controller/AuthController.java`, replace the `login` method and drop the `LoginRequest`/`LoginResponse` imports, adding `Session` and `RequestParam`:

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User created = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<Session> login(@RequestParam String email, @RequestParam String password) {
        return ResponseEntity.ok(authService.login(email, password));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=AuthServiceTest,AuthControllerTest test`
Expected: PASS (all)

- [ ] **Step 5: Delete the now-unused login DTOs and commit**

```bash
rm src/main/java/com/mgwprod/users/dto/LoginRequest.java src/main/java/com/mgwprod/users/dto/LoginResponse.java
git add -A src/main/java/com/mgwprod/users/service/AuthService.java src/main/java/com/mgwprod/users/controller/AuthController.java src/test/java/com/mgwprod/users/service/AuthServiceTest.java src/test/java/com/mgwprod/users/controller/AuthControllerTest.java src/main/java/com/mgwprod/users/dto/LoginRequest.java src/main/java/com/mgwprod/users/dto/LoginResponse.java
git commit -m "refactor(users): bind login endpoint to raw params, return Session directly"
```

---

### Task 5: `GET` user and profile without DTOs

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java`
- Modify: `src/main/java/com/mgwprod/users/controller/UserController.java`
- Modify: `src/test/java/com/mgwprod/users/service/UserServiceTest.java`
- Modify: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`

**Interfaces:**
- Produces: `UserService.getById(Long): User` (was already close to this shape, now returns the entity instead of `UserResponse`); new `UserService.getProfile(Long): Object` (returns `ProducerProfile` or `ArtistProfile` by role). `UserController` exposes `GET /api/users/{id}` and the new `GET /api/users/{id}/profile`.
- Note: `update`/`updateUser` still exist in `UserService` at the old two-DTO signature until Task 6 — this task only touches the `getById`/`getProfile` path and its own tests, leaving `update` and its tests untouched (they still compile against the old `UpdateUserRequest`/`UserResponse` until Task 6).

- [ ] **Step 1: Write the failing tests**

In `src/test/java/com/mgwprod/users/service/UserServiceTest.java`, replace the two `getById` tests:

```java
    @Test
    void getByIdReturnsUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User response = userService.getById(1L);

        assertEquals("productor@test.com", response.getEmail());
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    void getProfileReturnsProducerProfileForProducer() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("RKT,Trap");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        Object response = userService.getProfile(1L);

        assertEquals(profile, response);
    }

    @Test
    void getProfileReturnsArtistProfileForArtist() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setBio("bio");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        Object response = userService.getProfile(2L);

        assertEquals(profile, response);
    }
```

Add `import com.mgwprod.users.model.ArtistProfile;` to that file's imports.

In `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`, replace the two `getUser` tests and add a profile test:

```java
    @Test
    void getUserReturns200WithUserData() throws Exception {
        User response = new User();
        response.setId(1L);
        response.setDisplayName("DJ Test");
        response.setRole(Role.PRODUCER);
        response.setCity("Buenos Aires");

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

    @Test
    void getProfileReturns200WithProducerProfile() throws Exception {
        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("RKT");

        when(userService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").value("RKT"));
    }
```

Update that file's imports: drop `com.mgwprod.users.dto.ProducerProfileDto`; add `import com.mgwprod.users.model.ProducerProfile;` and `import com.mgwprod.users.model.User;` (the `UpdateUserRequest`-based update tests below stay as-is until Task 6, keep that import for now).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=UserServiceTest,UserControllerTest test`
Expected: FAIL — compile error, `UserService.getById` still returns `UserResponse`, no `getProfile` method yet, no `GET /api/users/{id}/profile` route.

- [ ] **Step 3: Write minimal implementation**

In `src/main/java/com/mgwprod/users/service/UserService.java`, replace `getById` and add `getProfile` (leave `update`/`toResponse` untouched for now — Task 6 replaces them):

```java
    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public Object getProfile(Long userId) {
        User user = getById(userId);
        if (user.getRole() == Role.PRODUCER) {
            return producerProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + userId));
        }
        return artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + userId));
    }
```

In `src/main/java/com/mgwprod/users/controller/UserController.java`, add the `getUser`/`getProfile` methods (leave `updateUser` wired to `UpdateUserRequest` for now):

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.dto.UpdateUserRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
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
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/{id}/profile")
    public Object getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                    @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                    @Valid @RequestBody UpdateUserRequest request) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para editar un perfil");
        }
        return userService.update(id, requestingUserId, request);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=UserServiceTest,UserControllerTest test`
Expected: PASS for the tests touched in this task; the pre-existing `update`-related tests in both files still pass unchanged (they still exercise the untouched `UpdateUserRequest` path).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/service/UserService.java src/main/java/com/mgwprod/users/controller/UserController.java src/test/java/com/mgwprod/users/service/UserServiceTest.java src/test/java/com/mgwprod/users/controller/UserControllerTest.java
git commit -m "refactor(users): expose User and profile GET endpoints directly"
```

---

### Task 6: Split `update` into three entity-direct endpoints

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java`
- Modify: `src/main/java/com/mgwprod/users/controller/UserController.java`
- Modify: `src/test/java/com/mgwprod/users/service/UserServiceTest.java`
- Modify: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`
- Delete: `src/main/java/com/mgwprod/users/dto/UpdateUserRequest.java`
- Delete: `src/main/java/com/mgwprod/users/dto/UserResponse.java`
- Delete: `src/main/java/com/mgwprod/users/dto/ProducerProfileDto.java`
- Delete: `src/main/java/com/mgwprod/users/dto/ArtistProfileDto.java`

**Interfaces:**
- Consumes: `User`, `ProducerProfile`, `ArtistProfile` (Tasks 1–2), `getById` (Task 5).
- Produces: `UserService.updateUser(Long, Long, User): User`, `UserService.updateProducerProfile(Long, Long, ProducerProfile): ProducerProfile`, `UserService.updateArtistProfile(Long, Long, ArtistProfile): ArtistProfile`.

- [ ] **Step 1: Write the failing tests**

Replace the update-related tests in `src/test/java/com/mgwprod/users/service/UserServiceTest.java`:

```java
    @Test
    void updateUserChangesDisplayNameForOwner() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("Old Name");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User request = new User();
        request.setDisplayName("New Name");

        User response = userService.updateUser(1L, 1L, request);

        assertEquals("New Name", response.getDisplayName());
    }

    @Test
    void updateUserThrowsForbiddenWhenEditingSomeoneElse() {
        User request = new User();
        request.setDisplayName("New Name");

        assertThrows(ForbiddenOperationException.class, () -> userService.updateUser(1L, 2L, request));
    }

    @Test
    void updateProducerProfileChangesGenresForOwner() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("Old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(producerProfileRepository.save(any(ProducerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProducerProfile request = new ProducerProfile();
        request.setGenres("RKT,Trap");

        ProducerProfile response = userService.updateProducerProfile(1L, 1L, request);

        assertEquals("RKT,Trap", response.getGenres());
    }

    @Test
    void updateProducerProfileThrowsForbiddenWhenUserIsNotProducer() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ARTIST);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProducerProfile request = new ProducerProfile();
        request.setGenres("RKT");

        assertThrows(ForbiddenOperationException.class,
                () -> userService.updateProducerProfile(1L, 1L, request));
    }

    @Test
    void updateArtistProfileChangesBioForOwner() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setBio("Old bio");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setBio("New bio");

        ArtistProfile response = userService.updateArtistProfile(2L, 2L, request);

        assertEquals("New bio", response.getBio());
    }
```

Update that file's imports: drop `com.mgwprod.users.dto.UpdateUserRequest`, `com.mgwprod.users.dto.UserResponse`; add `import static org.mockito.ArgumentMatchers.any;` if not already present (it is, from Task 5's `getProfile` tests only if you kept `any` — otherwise add it), and `import com.mgwprod.users.model.ArtistProfile;` if not already added in Task 5.

Replace the update-related tests in `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`:

```java
    @Test
    void updateUserReturns200WhenOwnerEditsOwnProfile() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        User response = new User();
        response.setId(1L);
        response.setDisplayName("Nuevo Nombre");
        response.setRole(Role.PRODUCER);

        when(userService.updateUser(eq(1L), eq(1L), any(User.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nuevo Nombre"));
    }

    @Test
    void updateUserReturns401WhenNoUserIdAttribute() throws Exception {
        User request = new User();
        request.setDisplayName("Nuevo Nombre");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProducerProfileReturns200WhenOwnerEditsOwnProfile() throws Exception {
        ProducerProfile request = new ProducerProfile();
        request.setGenres("RKT,Trap");

        ProducerProfile response = new ProducerProfile();
        response.setGenres("RKT,Trap");

        when(userService.updateProducerProfile(eq(1L), eq(1L), any(ProducerProfile.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1/producer-profile")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").value("RKT,Trap"));
    }
```

Update that file's imports: drop `com.mgwprod.users.dto.UpdateUserRequest` and `java.time.Instant` (no test in this file calls `Instant.now()` anymore after this rewrite); add `import com.mgwprod.users.model.ProducerProfile;` (`User` is already imported from Task 5).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=UserServiceTest,UserControllerTest test`
Expected: FAIL — compile error, `UserService.update`/`toResponse` still bound to the old DTOs, no `updateProducerProfile`/`updateArtistProfile`, no `/producer-profile` route.

- [ ] **Step 3: Write minimal implementation**

Replace the full contents of `src/main/java/com/mgwprod/users/service/UserService.java`:

```java
package com.mgwprod.users.service;

import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public Object getProfile(Long userId) {
        User user = getById(userId);
        if (user.getRole() == Role.PRODUCER) {
            return producerProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + userId));
        }
        return artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + userId));
    }

    @Transactional
    public User updateUser(Long targetUserId, Long requestingUserId, User request) {
        User user = requireOwnership(targetUserId, requestingUserId);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        return userRepository.save(user);
    }

    @Transactional
    public ProducerProfile updateProducerProfile(Long targetUserId, Long requestingUserId, ProducerProfile request) {
        User user = requireOwnership(targetUserId, requestingUserId);
        if (user.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de productor");
        }

        ProducerProfile profile = producerProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + targetUserId));
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
        return producerProfileRepository.save(profile);
    }

    @Transactional
    public ArtistProfile updateArtistProfile(Long targetUserId, Long requestingUserId, ArtistProfile request) {
        User user = requireOwnership(targetUserId, requestingUserId);
        if (user.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de artista");
        }

        ArtistProfile profile = artistProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + targetUserId));
        if (request.getGenres() != null) {
            profile.setGenres(request.getGenres());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        return artistProfileRepository.save(profile);
    }

    private User requireOwnership(Long targetUserId, Long requestingUserId) {
        if (!targetUserId.equals(requestingUserId)) {
            throw new ForbiddenOperationException("No podés editar el perfil de otro usuario");
        }
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
    }
}
```

Replace the full contents of `src/main/java/com/mgwprod/users/controller/UserController.java`:

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
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
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/{id}/profile")
    public Object getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id,
                            @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                            @RequestBody User request) {
        requireAuthenticated(requestingUserId);
        return userService.updateUser(id, requestingUserId, request);
    }

    @PutMapping("/{id}/producer-profile")
    public ProducerProfile updateProducerProfile(@PathVariable Long id,
                                                  @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                                  @Valid @RequestBody ProducerProfile request) {
        requireAuthenticated(requestingUserId);
        return userService.updateProducerProfile(id, requestingUserId, request);
    }

    @PutMapping("/{id}/artist-profile")
    public ArtistProfile updateArtistProfile(@PathVariable Long id,
                                              @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                              @Valid @RequestBody ArtistProfile request) {
        requireAuthenticated(requestingUserId);
        return userService.updateArtistProfile(id, requestingUserId, request);
    }

    private void requireAuthenticated(Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para editar un perfil");
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=UserServiceTest,UserControllerTest test`
Expected: PASS (all)

- [ ] **Step 5: Delete the now-unused DTOs and commit**

```bash
rm src/main/java/com/mgwprod/users/dto/UpdateUserRequest.java src/main/java/com/mgwprod/users/dto/UserResponse.java src/main/java/com/mgwprod/users/dto/ProducerProfileDto.java src/main/java/com/mgwprod/users/dto/ArtistProfileDto.java
rmdir src/main/java/com/mgwprod/users/dto
git add -A src/main/java/com/mgwprod/users/service/UserService.java src/main/java/com/mgwprod/users/controller/UserController.java src/test/java/com/mgwprod/users/service/UserServiceTest.java src/test/java/com/mgwprod/users/controller/UserControllerTest.java
git commit -m "refactor(users): split profile update into entity-direct endpoints, delete dto package"
```

---

### Task 7: Full-suite verification and manual smoke test

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `mvn test`
Expected: `BUILD SUCCESS`, all tests green (the pre-existing `PasswordHasherTest`, `SessionAuthInterceptorTest`, and `MgwProdApplicationTests` were never touched by this plan and must still pass unchanged).

- [ ] **Step 2: Confirm the dto package and all seven original files are gone**

Run: `find src/main/java/com/mgwprod/users/dto -type f 2>&1; echo "exit: $?"`
Expected: `find: ... No such file or directory` (directory removed in Task 6, Step 5) — confirms all seven DTO classes are deleted.

- [ ] **Step 3: Manual smoke test against a running instance**

Run: `mvn spring-boot:run` (leave running), then in a second terminal:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@test.com","password":"supersecret123","displayName":"Smoke Test","role":"ARTIST"}' | tee /tmp/register.json

curl -s -X POST "http://localhost:8080/api/auth/login?email=smoke@test.com&password=supersecret123" | tee /tmp/login.json

USER_ID=$(python3 -c "import json;print(json.load(open('/tmp/register.json'))['id'])")
curl -s http://localhost:8080/api/users/$USER_ID
curl -s http://localhost:8080/api/users/$USER_ID/profile
```

Expected:
- Register response is `201` with the created user's JSON — confirm with `grep -c passwordHash /tmp/register.json` and `grep -c '"password"' /tmp/register.json` both printing `0`.
- Login response is `200` with a JSON body containing `"token"` and a nested `"user"` object — confirm `grep -c passwordHash /tmp/login.json` prints `0`.
- `GET /api/users/$USER_ID` returns the user's JSON (no `passwordHash`/`password` keys).
- `GET /api/users/$USER_ID/profile` returns the `ArtistProfile` JSON (`genres`/`bio`, no nested `user` key).

Stop the running instance (Ctrl+C) once confirmed.

- [ ] **Step 4: Report back**

No commit for this task — it's verification only. Summarize the `mvn test` result and the four smoke-test checks to Santiago before considering the module aligned with Clase 4.

---

## Post-execution notes (2026-08-31)

All 7 tasks executed via `superpowers:subagent-driven-development`, verified end-to-end against a real local MySQL, final whole-branch review clean (no Critical findings). Full ruling history and per-task review outcomes are in the SDD run's ledger (not committed — see the git log on `refactor/users-remove-dtos` for the equivalent commit-by-commit record). Three things worth keeping visible beyond this branch, for whoever touches `catalog`/`orders`/`challenges` next:

1. **`spring.jpa.properties.jakarta.persistence.validation.mode=none` is now set in `application.properties` and applies to the whole app, not just `users`.** It was added because Hibernate validates `@NotBlank`/`@Size`/etc. on every entity save by default (insert AND update), and `User.password` (a `@Transient`, register-only field) is always `null` on any `User` loaded from the DB — so without this setting, every `PUT` that saves a `User` 500s. If you add Bean Validation annotations to a new `@Entity` in another module, they will **not** be enforced automatically on save anymore — validate at the controller with `@Valid @RequestBody`, the same pattern already used here, not by relying on JPA.
2. **`PUT /api/users/{id}` has no request validation** (no `@Valid`, no bean-validation annotations checked) — a blank-string `displayName`/`city` is silently accepted. This was already a deliberate tradeoff in the plan (avoiding a validation-group conflict between register's required fields and update's optional ones on the same `User` class); the validation-mode change above just means there's now truly zero enforcement on that path, where before there was an unintentional partial one. Not a security issue, just weaker input hygiene than the old DTO had.
3. **The two live-only bugs found by the Task 7 smoke test (register 500, update 500) have no automated regression test** — both fixes are 1-line/1-property changes with no new test, because the existing suite mocks `userRepository.save()` and structurally can't exercise real Hibernate persist-time behavior. If this project ever adds an H2- or Testcontainers-backed slice test, `AuthService.register` and `UserService.updateUser` are the first candidates — that's the only way to close this gap for real instead of relying on a manual smoke test to catch a recurrence.
