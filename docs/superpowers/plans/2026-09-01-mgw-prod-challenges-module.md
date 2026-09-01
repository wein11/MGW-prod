# Módulo `challenges` + verificación de productores — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el módulo `challenges` (desafíos con jurado ponderado, premios, ranking) y la extensión chica de `users` (flag `verified` en `ProducerProfile`) del pivot de mgw-prod. Dueño: Paolo. Es el módulo más pesado de los tres (9 tareas, incluye el cálculo ponderado y la orquestación del cierre con `ChallengeResult`) — arrancalo con margen de tiempo.

**Architecture:** Paquete vertical `com.mgwprod.challenges` (controller/service/repository/model/exception), mismo patrón que `users`/`catalog`/`collab`: sin DTOs, Service con la lógica, Repository `JpaRepository`. El cálculo del puntaje ponderado vive en una clase separada y pura (`ChallengeScoringService`, sin dependencias de base de datos) para poder testearla con inputs directos, sin mocks de repositorios. `challenges` solo referencia `User`/`ProducerProfile` (de `users`) por FK plana, nunca al revés.

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` (secciones "Módulo `challenges`", "Cálculo del puntaje ponderado" y "Endpoints").

## Global Constraints

- **Sin DTOs**: los controllers reciben y devuelven las entidades JPA directamente.
- **Validación solo en el controller**: `@Valid @RequestBody` siempre; Hibernate no valida al hacer `save()`.
- **Esquema manual**: `ddl-auto=none`. Sumar las tablas/columnas nuevas al final de `docs/db/schema.sql`.
- **Auth casera**: `SessionAuthInterceptor` setea `userId`/`userRole` como request attributes cuando hay Bearer token válido; si no hay token, el request pasa sin esos attributes.
- **Nunca commitear a `main` directo** — trabajo en una branch nueva (ver Prerequisito).
- **Sin batch/async**: el cierre del challenge (`close`) es un endpoint que dispara el admin a mano — no hay scheduler ni job en background, según el requisito de Etapa 1 ("tiempo real, sin batch/async").
- **La categoría del jurado no se persiste en `Vote`**: se resuelve en el momento del cálculo comparando `voterId` contra `Challenge.guestArtistId` y contra los productores con `ProducerProfile.verified = true`.
- **Si el artista invitado no votó una submission, esa porción del puntaje vale 0** — sin renormalización.

## Prerequisito (una sola vez, antes de la Task 1)

El PR de "sin DTOs" (`refactor/users-remove-dtos`, PR #1) ya está mergeado a `main` (2026-09-01) — este plan ya asume la forma post-refactor de `ProducerProfile`/`ProducerProfileRepository`.

```bash
git checkout main
git pull
git checkout -b feature/challenges-module
```

---

### Task 1: `verified` en `ProducerProfile` + `PUT /api/producers/{id}/verify`

**Files:**
- Modify: `src/main/java/com/mgwprod/users/model/ProducerProfile.java`
- Modify: `src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java`
- Modify: `docs/db/schema.sql`
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java`
- Create: `src/main/java/com/mgwprod/users/controller/ProducerVerificationController.java`
- Test: `src/test/java/com/mgwprod/users/service/UserServiceTest.java` (agregar casos)
- Test: `src/test/java/com/mgwprod/users/controller/ProducerVerificationControllerTest.java`

**Interfaces:**
- Produces: `ProducerProfile.isVerified()`/`.setVerified(boolean)`, `ProducerProfileRepository.findByVerifiedTrue()`, `UserService.verifyProducer(Long requestingUserId, Long producerId)`.

- [ ] **Step 1: Agregar la columna al schema**

Sumar al final de `docs/db/schema.sql`:

```sql
ALTER TABLE producer_profiles ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;
```

- [ ] **Step 2: Escribir los tests de servicio (fallan al no existir el método)**

Sumar a `UserServiceTest.java`:

```java
    @Test
    void verifyProducerSetsVerifiedTrueWhenRequesterIsAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        ProducerProfile profile = new ProducerProfile();
        profile.setVerified(false);
        when(producerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(producerProfileRepository.save(profile)).thenReturn(profile);

        ProducerProfile result = userService.verifyProducer(1L, 2L);

        assertThat(result.isVerified()).isTrue();
    }

    @Test
    void verifyProducerThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        assertThatThrownBy(() -> userService.verifyProducer(1L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
```

Agregar los imports que falten (`ProducerProfile`, `ForbiddenOperationException`, `assertThatThrownBy` de `org.assertj.core.api.Assertions`) si `UserServiceTest.java` todavía no los tiene.

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=UserServiceTest`
Expected: FAIL (no compila — `verifyProducer` no existe).

- [ ] **Step 4: Agregar `verified` a `ProducerProfile` y el finder al repositorio**

En `ProducerProfile.java`, sumar el campo (mismo estilo que el resto de la clase):

```java
    @Column(nullable = false)
    private boolean verified = false;
```

En `ProducerProfileRepository.java`, sumar:

```java
    java.util.List<ProducerProfile> findByVerifiedTrue();
```

- [ ] **Step 5: Implementar `UserService.verifyProducer`**

Sumar a `UserService.java`:

```java
    @Transactional
    public ProducerProfile verifyProducer(Long requestingUserId, Long producerId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (!requester.isAdmin()) {
            throw new ForbiddenOperationException("Solo un admin puede verificar productores");
        }
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Solo se puede verificar a un productor");
        }
        ProducerProfile profile = producerProfileRepository.findByUserId(producerId)
                .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + producerId));
        profile.setVerified(true);
        return producerProfileRepository.save(profile);
    }
```

- [ ] **Step 6: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=UserServiceTest`
Expected: PASS

- [ ] **Step 7: Escribir el test de controller**

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProducerVerificationController.class)
class ProducerVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void verifyReturns200WhenRequesterIsAdmin() throws Exception {
        ProducerProfile profile = new ProducerProfile();
        profile.setVerified(true);

        when(userService.verifyProducer(eq(1L), eq(2L))).thenReturn(profile);

        mockMvc.perform(put("/api/producers/2/verify").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void verifyReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/producers/2/verify"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 8: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ProducerVerificationControllerTest`
Expected: FAIL

- [ ] **Step 9: Implementar `ProducerVerificationController`**

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.service.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerVerificationController {

    private final UserService userService;

    public ProducerVerificationController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/api/producers/{id}/verify")
    public ProducerProfile verify(@PathVariable Long id,
                                   @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para verificar un productor");
        }
        return userService.verifyProducer(requestingUserId, id);
    }
}
```

- [ ] **Step 10: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ProducerVerificationControllerTest,UserServiceTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/users/model/ProducerProfile.java \
        src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java \
        src/main/java/com/mgwprod/users/service/UserService.java \
        src/main/java/com/mgwprod/users/controller/ProducerVerificationController.java \
        src/test/java/com/mgwprod/users/service/UserServiceTest.java \
        src/test/java/com/mgwprod/users/controller/ProducerVerificationControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(users): add producer verification flag and admin endpoint"
```

---

### Task 2: Entidad `Challenge` + repositorio + schema

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/model/Challenge.java`
- Create: `src/main/java/com/mgwprod/challenges/repository/ChallengeRepository.java`
- Create: `src/main/java/com/mgwprod/challenges/exception/ChallengeNotFoundException.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/challenges/repository/ChallengeRepositoryTest.java`

**Interfaces:**
- Produces: `Challenge` (id, title, genre, bpm, key, theme, deadline, guestArtistId, prizeFirst, prizeSecond, prizeThird, opportunityPickSubmissionId, createdAt), `ChallengeRepository`, `ChallengeNotFoundException(Long)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    bpm INT NOT NULL,
    music_key VARCHAR(20),
    theme VARCHAR(255),
    deadline DATETIME NOT NULL,
    guest_artist_id BIGINT NOT NULL,
    prize_first VARCHAR(255),
    prize_second VARCHAR(255),
    prize_third VARCHAR(255),
    opportunity_pick_submission_id BIGINT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (guest_artist_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class ChallengeRepositoryTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Test
    void savesAndReturnsChallenge() {
        Challenge challenge = new Challenge();
        challenge.setTitle("Creamos el próximo hit de RKT");
        challenge.setGenre("RKT");
        challenge.setBpm(100);
        challenge.setTheme("libre");
        challenge.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        challenge.setGuestArtistId(1L);

        Challenge saved = challengeRepository.save(challenge);

        assertThat(saved.getId()).isNotNull();
        assertThat(challengeRepository.findById(saved.getId())).isPresent();
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear la entidad**

```java
package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "El género es obligatorio")
    @Column(nullable = false)
    private String genre;

    @NotNull(message = "El BPM es obligatorio")
    @Min(value = 1, message = "El BPM debe ser mayor a 0")
    @Column(nullable = false)
    private Integer bpm;

    @Column(name = "music_key")
    private String key;

    private String theme;

    @NotNull(message = "El deadline es obligatorio")
    @Column(nullable = false)
    private Instant deadline;

    @NotNull(message = "El artista invitado es obligatorio")
    @Column(name = "guest_artist_id", nullable = false)
    private Long guestArtistId;

    @Column(name = "prize_first")
    private String prizeFirst;

    @Column(name = "prize_second")
    private String prizeSecond;

    @Column(name = "prize_third")
    private String prizeThird;

    @Column(name = "opportunity_pick_submission_id")
    private Long opportunityPickSubmissionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

- [ ] **Step 5: Crear repositorio y excepción**

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
}
```

```java
package com.mgwprod.challenges.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ChallengeNotFoundException extends ApiException {
    public ChallengeNotFoundException(Long challengeId) {
        super(HttpStatus.NOT_FOUND, "No existe un challenge con id: " + challengeId);
    }
}
```

- [ ] **Step 6: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ChallengeRepositoryTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/model/Challenge.java \
        src/main/java/com/mgwprod/challenges/repository/ChallengeRepository.java \
        src/main/java/com/mgwprod/challenges/exception/ChallengeNotFoundException.java \
        src/test/java/com/mgwprod/challenges/repository/ChallengeRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(challenges): add Challenge entity, repository, and schema"
```

---

### Task 3: `POST /api/challenges`, `GET /api/challenges`, `GET /api/challenges/{id}`

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/service/ChallengeService.java`
- Create: `src/main/java/com/mgwprod/challenges/controller/ChallengeController.java`
- Test: `src/test/java/com/mgwprod/challenges/service/ChallengeServiceTest.java`
- Test: `src/test/java/com/mgwprod/challenges/controller/ChallengeControllerTest.java`

**Interfaces:**
- Consumes: `Challenge`/`ChallengeRepository`/`ChallengeNotFoundException` (Task 2), `com.mgwprod.users.repository.UserRepository` + `com.mgwprod.users.model.{User,Role}` + `com.mgwprod.users.exception.{UserNotFoundException, ForbiddenOperationException, UnauthenticatedException}`.
- Produces: `ChallengeService.create(Long requestingUserId, Challenge challenge)`, `.list()`, `.getById(Long)`.

- [ ] **Step 1: Escribir el test de servicio**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void createSavesChallengeWhenRequesterIsAdminAndGuestIsArtist() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User guestArtist = new User();
        guestArtist.setId(2L);
        guestArtist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guestArtist));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);
        challenge.setDeadline(Instant.now().plusSeconds(604800));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge saved = challengeService.create(1L, challenge);

        assertThat(saved.getGuestArtistId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);

        assertThatThrownBy(() -> challengeService.create(1L, challenge))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createThrowsWhenGuestArtistIsNotAnArtist() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User notArtist = new User();
        notArtist.setId(2L);
        notArtist.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notArtist));

        Challenge challenge = new Challenge();
        challenge.setGuestArtistId(2L);

        assertThatThrownBy(() -> challengeService.create(1L, challenge))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeServiceTest`
Expected: FAIL

- [ ] **Step 3: Implementar `ChallengeService` (create/list/getById)**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.exception.ChallengeNotFoundException;
import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    public ChallengeService(ChallengeRepository challengeRepository, UserRepository userRepository) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Challenge create(Long requestingUserId, Challenge challenge) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (!requester.isAdmin()) {
            throw new ForbiddenOperationException("Solo un admin puede crear challenges");
        }
        User guestArtist = userRepository.findById(challenge.getGuestArtistId())
                .orElseThrow(() -> new UserNotFoundException(challenge.getGuestArtistId()));
        if (guestArtist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("El artista invitado debe tener rol ARTIST");
        }
        return challengeRepository.save(challenge);
    }

    @Transactional(readOnly = true)
    public List<Challenge> list() {
        return challengeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Challenge getById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException(id));
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ChallengeServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.service.ChallengeService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeController.class)
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChallengeService challengeService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createChallengeReturns201WhenAuthenticated() throws Exception {
        Challenge request = new Challenge();
        request.setTitle("Creamos el próximo hit de RKT");
        request.setGenre("RKT");
        request.setBpm(100);
        request.setDeadline(Instant.now().plusSeconds(604800));
        request.setGuestArtistId(2L);

        Challenge response = new Challenge();
        response.setId(1L);
        response.setTitle("Creamos el próximo hit de RKT");

        when(challengeService.create(eq(1L), any(Challenge.class))).thenReturn(response);

        mockMvc.perform(post("/api/challenges")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Creamos el próximo hit de RKT"));
    }

    @Test
    void createChallengeReturns401WhenNotAuthenticated() throws Exception {
        Challenge request = new Challenge();
        request.setTitle("Creamos el próximo hit de RKT");

        mockMvc.perform(post("/api/challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listChallengesReturns200() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(1L);

        when(challengeService.list()).thenReturn(java.util.List.of(challenge));

        mockMvc.perform(get("/api/challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getChallengeReturns200WhenExists() throws Exception {
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setTitle("Creamos el próximo hit de RKT");

        when(challengeService.getById(1L)).thenReturn(challenge);

        mockMvc.perform(get("/api/challenges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Creamos el próximo hit de RKT"));
    }
}
```

- [ ] **Step 6: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeControllerTest`
Expected: FAIL

- [ ] **Step 7: Implementar `ChallengeController`**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.service.ChallengeService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @PostMapping
    public ResponseEntity<Challenge> createChallenge(@RequestAttribute(name = "userId", required = false) Long userId,
                                                      @Valid @RequestBody Challenge challenge) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para crear un challenge");
        }
        Challenge created = challengeService.create(userId, challenge);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Challenge> listChallenges() {
        return challengeService.list();
    }

    @GetMapping("/{id}")
    public Challenge getChallenge(@PathVariable Long id) {
        return challengeService.getById(id);
    }
}
```

Nota: la spec original agrupaba "detalle + submissions" en un solo endpoint; acá se separa (`GET /api/challenges/{id}` solo el challenge, `GET /api/challenges/{id}/submissions` aparte en la Task 4) para respetar la regla de "sin DTOs" — cada endpoint devuelve una sola entidad o lista de una entidad, nunca un objeto combinado ad hoc.

- [ ] **Step 8: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ChallengeControllerTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/service/ChallengeService.java \
        src/main/java/com/mgwprod/challenges/controller/ChallengeController.java \
        src/test/java/com/mgwprod/challenges/service/ChallengeServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/ChallengeControllerTest.java
git commit -m "feat(challenges): add POST/GET /api/challenges"
```

---

### Task 4: `Submission` + `POST/GET /api/challenges/{id}/submissions`

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/model/Submission.java`
- Create: `src/main/java/com/mgwprod/challenges/repository/SubmissionRepository.java`
- Create: `src/main/java/com/mgwprod/challenges/exception/SubmissionNotFoundException.java`
- Modify: `docs/db/schema.sql`
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeService.java` (agregar `SubmissionService` es más prolijo — ver abajo)
- Create: `src/main/java/com/mgwprod/challenges/service/SubmissionService.java`
- Create: `src/main/java/com/mgwprod/challenges/controller/SubmissionController.java`
- Test: `src/test/java/com/mgwprod/challenges/repository/SubmissionRepositoryTest.java`
- Test: `src/test/java/com/mgwprod/challenges/service/SubmissionServiceTest.java`
- Test: `src/test/java/com/mgwprod/challenges/controller/SubmissionControllerTest.java`

**Interfaces:**
- Produces: `Submission` (id, challengeId, producerId, audioUrl, submittedAt), `SubmissionRepository.findByChallengeId(Long)`, `.findByProducerId(Long)`, `SubmissionService.create(Long challengeId, Long producerId, Submission submission)`, `.listByChallenge(Long challengeId)`, `.getById(Long)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    producer_id BIGINT NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    submitted_at DATETIME NOT NULL,
    FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    FOREIGN KEY (producer_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Submission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Test
    void findByChallengeIdReturnsOnlyThatChallengesSubmissions() {
        Submission submission = new Submission();
        submission.setChallengeId(1L);
        submission.setProducerId(2L);
        submission.setAudioUrl("https://soundcloud.com/example/submission");
        submissionRepository.save(submission);

        List<Submission> result = submissionRepository.findByChallengeId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProducerId()).isEqualTo(2L);
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubmissionRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear entidad, repositorio y excepción**

```java
package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El challenge es obligatorio")
    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @NotNull(message = "El producer es obligatorio")
    @Column(name = "producer_id", nullable = false)
    private Long producerId;

    @NotBlank(message = "El link de audio es obligatorio")
    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }
}
```

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByChallengeId(Long challengeId);
    List<Submission> findByProducerId(Long producerId);
}
```

```java
package com.mgwprod.challenges.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SubmissionNotFoundException extends ApiException {
    public SubmissionNotFoundException(Long submissionId) {
        super(HttpStatus.NOT_FOUND, "No existe una submission con id: " + submissionId);
    }
}
```

- [ ] **Step 5: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=SubmissionRepositoryTest`
Expected: PASS

- [ ] **Step 6: Escribir el test de `SubmissionService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChallengeService challengeService;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void createSavesSubmissionWhenProducerAndDeadlineAreValid() {
        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDeadline(Instant.now().plusSeconds(3600));
        when(challengeService.getById(1L)).thenReturn(challenge);

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission saved = submissionService.create(1L, 2L, submission);

        assertThat(saved.getChallengeId()).isEqualTo(1L);
        assertThat(saved.getProducerId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenDeadlineHasPassed() {
        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDeadline(Instant.now().minusSeconds(3600));
        when(challengeService.getById(1L)).thenReturn(challenge);

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");

        assertThatThrownBy(() -> submissionService.create(1L, 2L, submission))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createThrowsWhenUserIsNotProducer() {
        User artist = new User();
        artist.setId(2L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));

        Submission submission = new Submission();
        submission.setAudioUrl("https://soundcloud.com/example/submission");

        assertThatThrownBy(() -> submissionService.create(1L, 2L, submission))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 7: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubmissionServiceTest`
Expected: FAIL

- [ ] **Step 8: Implementar `SubmissionService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.exception.SubmissionNotFoundException;
import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ChallengeService challengeService;

    public SubmissionService(SubmissionRepository submissionRepository,
                              UserRepository userRepository,
                              ChallengeService challengeService) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.challengeService = challengeService;
    }

    @Transactional
    public Submission create(Long challengeId, Long producerId, Submission submission) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Solo un productor puede enviar una submission");
        }
        Challenge challenge = challengeService.getById(challengeId);
        if (Instant.now().isAfter(challenge.getDeadline())) {
            throw new ForbiddenOperationException("El deadline de este challenge ya pasó");
        }
        submission.setChallengeId(challengeId);
        submission.setProducerId(producerId);
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> listByChallenge(Long challengeId) {
        return submissionRepository.findByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public Submission getById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }
}
```

- [ ] **Step 9: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=SubmissionServiceTest`
Expected: PASS

- [ ] **Step 10: Escribir el test de controller**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.service.SubmissionService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createSubmissionReturns201WhenAuthenticated() throws Exception {
        Submission request = new Submission();
        request.setAudioUrl("https://soundcloud.com/example/submission");

        Submission response = new Submission();
        response.setId(1L);
        response.setChallengeId(1L);
        response.setProducerId(2L);

        when(submissionService.create(eq(1L), eq(2L), any(Submission.class))).thenReturn(response);

        mockMvc.perform(post("/api/challenges/1/submissions")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.producerId").value(2));
    }

    @Test
    void createSubmissionReturns401WhenNotAuthenticated() throws Exception {
        Submission request = new Submission();
        request.setAudioUrl("https://soundcloud.com/example/submission");

        mockMvc.perform(post("/api/challenges/1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSubmissionsReturns200() throws Exception {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setChallengeId(1L);

        when(submissionService.listByChallenge(1L)).thenReturn(java.util.List.of(submission));

        mockMvc.perform(get("/api/challenges/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].challengeId").value(1));
    }
}
```

- [ ] **Step 11: Correr, verificar que falla**

Run: `./mvnw test -Dtest=SubmissionControllerTest`
Expected: FAIL

- [ ] **Step 12: Implementar `SubmissionController`**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.service.SubmissionService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/challenges/{challengeId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<Submission> createSubmission(@PathVariable Long challengeId,
                                                         @RequestAttribute(name = "userId", required = false) Long userId,
                                                         @Valid @RequestBody Submission submission) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para enviar una submission");
        }
        Submission created = submissionService.create(challengeId, userId, submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Submission> listSubmissions(@PathVariable Long challengeId) {
        return submissionService.listByChallenge(challengeId);
    }
}
```

- [ ] **Step 13: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=SubmissionControllerTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/model/Submission.java \
        src/main/java/com/mgwprod/challenges/repository/SubmissionRepository.java \
        src/main/java/com/mgwprod/challenges/exception/SubmissionNotFoundException.java \
        src/main/java/com/mgwprod/challenges/service/SubmissionService.java \
        src/main/java/com/mgwprod/challenges/controller/SubmissionController.java \
        src/test/java/com/mgwprod/challenges/repository/SubmissionRepositoryTest.java \
        src/test/java/com/mgwprod/challenges/service/SubmissionServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/SubmissionControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(challenges): add submissions endpoints"
```

---

### Task 5: `Vote` + `POST /api/submissions/{id}/votes`

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/model/Vote.java`
- Create: `src/main/java/com/mgwprod/challenges/repository/VoteRepository.java`
- Modify: `docs/db/schema.sql`
- Create: `src/main/java/com/mgwprod/challenges/service/VoteService.java`
- Create: `src/main/java/com/mgwprod/challenges/controller/VoteController.java`
- Test: `src/test/java/com/mgwprod/challenges/repository/VoteRepositoryTest.java`
- Test: `src/test/java/com/mgwprod/challenges/service/VoteServiceTest.java`
- Test: `src/test/java/com/mgwprod/challenges/controller/VoteControllerTest.java`

**Interfaces:**
- Produces: `Vote` (id, submissionId, voterId, score, comment), `VoteRepository.findBySubmissionId(Long)`, `.existsBySubmissionIdAndVoterId(Long, Long)`, `VoteService.create(Long submissionId, Long voterId, Vote vote)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    voter_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(1000),
    UNIQUE (submission_id, voter_id),
    FOREIGN KEY (submission_id) REFERENCES submissions(id),
    FOREIGN KEY (voter_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Vote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class VoteRepositoryTest {

    @Autowired
    private VoteRepository voteRepository;

    @Test
    void findBySubmissionIdReturnsOnlyThatSubmissionsVotes() {
        Vote vote = new Vote();
        vote.setSubmissionId(1L);
        vote.setVoterId(2L);
        vote.setScore(8);
        voteRepository.save(vote);

        List<Vote> result = voteRepository.findBySubmissionId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(8);
    }

    @Test
    void existsBySubmissionIdAndVoterIdDetectsDuplicateVote() {
        Vote vote = new Vote();
        vote.setSubmissionId(1L);
        vote.setVoterId(2L);
        vote.setScore(8);
        voteRepository.save(vote);

        assertThat(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).isTrue();
        assertThat(voteRepository.existsBySubmissionIdAndVoterId(1L, 999L)).isFalse();
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=VoteRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear entidad y repositorio**

```java
package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La submission es obligatoria")
    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @NotNull(message = "El votante es obligatorio")
    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @NotNull(message = "El puntaje es obligatorio")
    @Min(value = 1, message = "El puntaje mínimo es 1")
    @Max(value = 10, message = "El puntaje máximo es 10")
    @Column(nullable = false)
    private Integer score;

    @Column(length = 1000)
    private String comment;
}
```

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndVoterId(Long submissionId, Long voterId);
}
```

- [ ] **Step 5: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=VoteRepositoryTest`
Expected: PASS

- [ ] **Step 6: Escribir el test de `VoteService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private VoteService voteService;

    @Test
    void createSavesVoteWhenSubmissionExistsAndNoDuplicate() {
        Submission submission = new Submission();
        submission.setId(1L);
        when(submissionService.getById(1L)).thenReturn(submission);
        when(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).thenReturn(false);

        Vote vote = new Vote();
        vote.setScore(9);
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vote saved = voteService.create(1L, 2L, vote);

        assertThat(saved.getSubmissionId()).isEqualTo(1L);
        assertThat(saved.getVoterId()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenVoterAlreadyVotedThisSubmission() {
        Submission submission = new Submission();
        submission.setId(1L);
        when(submissionService.getById(1L)).thenReturn(submission);
        when(voteRepository.existsBySubmissionIdAndVoterId(1L, 2L)).thenReturn(true);

        Vote vote = new Vote();
        vote.setScore(9);

        assertThatThrownBy(() -> voteService.create(1L, 2L, vote))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 7: Correr, verificar que falla**

Run: `./mvnw test -Dtest=VoteServiceTest`
Expected: FAIL

- [ ] **Step 8: Implementar `VoteService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final SubmissionService submissionService;

    public VoteService(VoteRepository voteRepository, SubmissionService submissionService) {
        this.voteRepository = voteRepository;
        this.submissionService = submissionService;
    }

    @Transactional
    public Vote create(Long submissionId, Long voterId, Vote vote) {
        submissionService.getById(submissionId); // valida que la submission exista
        if (voteRepository.existsBySubmissionIdAndVoterId(submissionId, voterId)) {
            throw new ForbiddenOperationException("Ya votaste esta submission");
        }
        vote.setSubmissionId(submissionId);
        vote.setVoterId(voterId);
        return voteRepository.save(vote);
    }
}
```

- [ ] **Step 9: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=VoteServiceTest`
Expected: PASS

- [ ] **Step 10: Escribir el test de controller e implementar `VoteController`**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.service.VoteService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoteService voteService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createVoteReturns201WhenAuthenticated() throws Exception {
        Vote request = new Vote();
        request.setScore(9);

        Vote response = new Vote();
        response.setId(1L);
        response.setSubmissionId(1L);
        response.setVoterId(2L);
        response.setScore(9);

        when(voteService.create(eq(1L), eq(2L), any(Vote.class))).thenReturn(response);

        mockMvc.perform(post("/api/submissions/1/votes")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(9));
    }

    @Test
    void createVoteReturns401WhenNotAuthenticated() throws Exception {
        Vote request = new Vote();
        request.setScore(9);

        mockMvc.perform(post("/api/submissions/1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
```

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.service.VoteService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions/{submissionId}/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<Vote> createVote(@PathVariable Long submissionId,
                                            @RequestAttribute(name = "userId", required = false) Long userId,
                                            @Valid @RequestBody Vote vote) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para votar");
        }
        Vote created = voteService.create(submissionId, userId, vote);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

- [ ] **Step 11: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=VoteControllerTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/model/Vote.java \
        src/main/java/com/mgwprod/challenges/repository/VoteRepository.java \
        src/main/java/com/mgwprod/challenges/service/VoteService.java \
        src/main/java/com/mgwprod/challenges/controller/VoteController.java \
        src/test/java/com/mgwprod/challenges/repository/VoteRepositoryTest.java \
        src/test/java/com/mgwprod/challenges/service/VoteServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/VoteControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(challenges): add votes endpoint with one-vote-per-submission rule"
```

---

### Task 6: `ChallengeScoringService` — cálculo puro del puntaje ponderado

Esta clase no toca la base de datos: recibe los votos ya cargados y devuelve el puntaje. Así se puede testear con inputs directos, sin mockear repositorios — es el corazón del diferencial del proyecto, conviene que quede blindado con tests.

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/service/ChallengeScoringService.java`
- Test: `src/test/java/com/mgwprod/challenges/service/ChallengeScoringServiceTest.java`

**Interfaces:**
- Produces: `ChallengeScoringService.computeScore(Long guestArtistId, java.util.Set<Long> verifiedProducerIds, java.util.List<Vote> votes)` → `double`.

- [ ] **Step 1: Escribir los tests (fallan al no existir la clase)**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChallengeScoringServiceTest {

    private final ChallengeScoringService scoringService = new ChallengeScoringService();

    private Vote voteFrom(Long voterId, int score) {
        Vote vote = new Vote();
        vote.setVoterId(voterId);
        vote.setScore(score);
        return vote;
    }

    @Test
    void weightsCommunityVerifiedAndGuestCorrectly() {
        // comunidad: voterId 1 y 2, promedio (6+8)/2 = 7.0 -> 0.30 * 7.0 = 2.1
        // verificados: voterId 3, promedio 9.0 -> 0.30 * 9.0 = 2.7
        // invitado: voterId 99, score 10 -> 0.40 * 10 = 4.0
        // total esperado: 2.1 + 2.7 + 4.0 = 8.8
        List<Vote> votes = List.of(
                voteFrom(1L, 6),
                voteFrom(2L, 8),
                voteFrom(3L, 9),
                voteFrom(99L, 10)
        );

        double score = scoringService.computeScore(99L, Set.of(3L), votes);

        assertThat(score).isCloseTo(8.8, within(0.001));
    }

    @Test
    void guestArtistPortionIsZeroWhenGuestDidNotVote() {
        // comunidad: voterId 1, score 10 -> 0.30 * 10 = 3.0
        // verificados: sin votos -> 0.30 * 0 = 0.0
        // invitado: no votó -> 0.40 * 0 = 0.0
        // total esperado: 3.0
        List<Vote> votes = List.of(voteFrom(1L, 10));

        double score = scoringService.computeScore(99L, Set.of(), votes);

        assertThat(score).isCloseTo(3.0, within(0.001));
    }

    @Test
    void returnsZeroWhenThereAreNoVotesAtAll() {
        double score = scoringService.computeScore(99L, Set.of(), List.of());

        assertThat(score).isCloseTo(0.0, within(0.001));
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeScoringServiceTest`
Expected: FAIL (no compila)

- [ ] **Step 3: Implementar `ChallengeScoringService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ChallengeScoringService {

    private static final double COMMUNITY_WEIGHT = 0.30;
    private static final double VERIFIED_WEIGHT = 0.30;
    private static final double GUEST_WEIGHT = 0.40;

    public double computeScore(Long guestArtistId, Set<Long> verifiedProducerIds, List<Vote> votes) {
        List<Integer> communityScores = new ArrayList<>();
        List<Integer> verifiedScores = new ArrayList<>();
        Integer guestScore = null;

        for (Vote vote : votes) {
            if (vote.getVoterId().equals(guestArtistId)) {
                guestScore = vote.getScore();
            } else if (verifiedProducerIds.contains(vote.getVoterId())) {
                verifiedScores.add(vote.getScore());
            } else {
                communityScores.add(vote.getScore());
            }
        }

        double communityAvg = average(communityScores);
        double verifiedAvg = average(verifiedScores);
        double guestComponent = guestScore != null ? guestScore : 0.0;

        return COMMUNITY_WEIGHT * communityAvg + VERIFIED_WEIGHT * verifiedAvg + GUEST_WEIGHT * guestComponent;
    }

    private double average(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ChallengeScoringServiceTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/service/ChallengeScoringService.java \
        src/test/java/com/mgwprod/challenges/service/ChallengeScoringServiceTest.java
git commit -m "feat(challenges): add weighted jury scoring calculation"
```

---

### Task 7: `ChallengeResult` + `PUT /api/challenges/{id}/close`

Cierra el challenge: calcula el puntaje de cada submission con `ChallengeScoringService`, ordena, arma el top 3, y persiste un `ChallengeResult` por cada uno. El ganador (#1) además queda verificado automáticamente (uno de los premios del pitch original).

**Files:**
- Create: `src/main/java/com/mgwprod/challenges/model/ChallengeResult.java`
- Create: `src/main/java/com/mgwprod/challenges/repository/ChallengeResultRepository.java`
- Modify: `docs/db/schema.sql`
- Create: `src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java`
- Create: `src/main/java/com/mgwprod/challenges/controller/ChallengeCloseController.java`
- Test: `src/test/java/com/mgwprod/challenges/repository/ChallengeResultRepositoryTest.java`
- Test: `src/test/java/com/mgwprod/challenges/service/ChallengeResultServiceTest.java`
- Test: `src/test/java/com/mgwprod/challenges/controller/ChallengeCloseControllerTest.java`

**Interfaces:**
- Consumes: `ChallengeScoringService.computeScore` (Task 6), `SubmissionRepository`, `VoteRepository`, `ChallengeService.getById`, `com.mgwprod.users.repository.{UserRepository, ProducerProfileRepository}`.
- Produces: `ChallengeResult` (id, challengeId, submissionId, rank, pointsAwarded, badge, prizeText), `ChallengeResultService.close(Long challengeId, Long requestingUserId)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE challenge_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL UNIQUE,
    rank_position INT NOT NULL,
    points_awarded INT NOT NULL,
    badge VARCHAR(255),
    prize_text VARCHAR(255),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    FOREIGN KEY (submission_id) REFERENCES submissions(id)
);
```

Nota: la columna se llama `rank_position` porque `RANK` es palabra reservada en MySQL.

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.ChallengeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class ChallengeResultRepositoryTest {

    @Autowired
    private ChallengeResultRepository challengeResultRepository;

    @Test
    void findByChallengeIdReturnsOnlyThatChallengesResults() {
        ChallengeResult result = new ChallengeResult();
        result.setChallengeId(1L);
        result.setSubmissionId(10L);
        result.setRank(1);
        result.setPointsAwarded(500);
        challengeResultRepository.save(result);

        List<ChallengeResult> found = challengeResultRepository.findByChallengeId(1L);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPointsAwarded()).isEqualTo(500);
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeResultRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear entidad y repositorio**

```java
package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "challenge_results")
@Getter
@Setter
@NoArgsConstructor
public class ChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;

    @Column(name = "rank_position", nullable = false)
    private Integer rank;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    private String badge;

    @Column(name = "prize_text")
    private String prizeText;
}
```

```java
package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.ChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeResultRepository extends JpaRepository<ChallengeResult, Long> {
    List<ChallengeResult> findByChallengeId(Long challengeId);
    List<ChallengeResult> findBySubmissionIdIn(List<Long> submissionIds);
}
```

- [ ] **Step 5: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ChallengeResultRepositoryTest`
Expected: PASS

- [ ] **Step 6: Escribir el test de `ChallengeResultService.close`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeResultServiceTest {

    @Mock
    private ChallengeResultRepository challengeResultRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProducerProfileRepository producerProfileRepository;

    @InjectMocks
    private ChallengeResultService challengeResultService;

    @Test
    void closeCreatesTopThreeResultsOrderedByScore() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);
        when(challengeService.getById(100L)).thenReturn(challenge);

        Submission low = new Submission();
        low.setId(1L);
        low.setProducerId(11L);
        Submission mid = new Submission();
        mid.setId(2L);
        mid.setProducerId(12L);
        Submission high = new Submission();
        high.setId(3L);
        high.setProducerId(13L);
        when(submissionRepository.findByChallengeId(100L)).thenReturn(List.of(low, mid, high));

        when(voteRepository.findBySubmissionId(1L)).thenReturn(List.of(voteFrom(1L, 3)));
        when(voteRepository.findBySubmissionId(2L)).thenReturn(List.of(voteFrom(1L, 6)));
        when(voteRepository.findBySubmissionId(3L)).thenReturn(List.of(voteFrom(1L, 9)));

        when(producerProfileRepository.findByVerifiedTrue()).thenReturn(List.of());

        ProducerProfile winnerProfile = new ProducerProfile();
        when(producerProfileRepository.findByUserId(13L)).thenReturn(Optional.of(winnerProfile));
        when(producerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(challengeResultRepository.save(any(ChallengeResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ChallengeResult> results = challengeResultService.close(100L, 1L);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getSubmissionId()).isEqualTo(3L);
        assertThat(results.get(0).getRank()).isEqualTo(1);
        assertThat(results.get(0).getPointsAwarded()).isEqualTo(500);
        assertThat(results.get(1).getSubmissionId()).isEqualTo(2L);
        assertThat(results.get(1).getPointsAwarded()).isEqualTo(300);
        assertThat(results.get(2).getSubmissionId()).isEqualTo(1L);
        assertThat(results.get(2).getPointsAwarded()).isEqualTo(150);
        assertThat(winnerProfile.isVerified()).isTrue();
    }

    @Test
    void closeThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        assertThatThrownBy(() -> challengeResultService.close(100L, 1L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private Vote voteFrom(Long voterId, int score) {
        Vote vote = new Vote();
        vote.setVoterId(voterId);
        vote.setScore(score);
        return vote;
    }
}
```

- [ ] **Step 7: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeResultServiceTest`
Expected: FAIL

- [ ] **Step 8: Implementar `ChallengeResultService`**

```java
package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChallengeResultService {

    private static final int[] POINTS_BY_RANK = {500, 300, 150};
    private static final int TOP_N = 3;

    private final ChallengeResultRepository challengeResultRepository;
    private final SubmissionRepository submissionRepository;
    private final VoteRepository voteRepository;
    private final ChallengeService challengeService;
    private final ChallengeScoringService challengeScoringService;
    private final UserRepository userRepository;
    private final ProducerProfileRepository producerProfileRepository;

    public ChallengeResultService(ChallengeResultRepository challengeResultRepository,
                                   SubmissionRepository submissionRepository,
                                   VoteRepository voteRepository,
                                   ChallengeService challengeService,
                                   ChallengeScoringService challengeScoringService,
                                   UserRepository userRepository,
                                   ProducerProfileRepository producerProfileRepository) {
        this.challengeResultRepository = challengeResultRepository;
        this.submissionRepository = submissionRepository;
        this.voteRepository = voteRepository;
        this.challengeService = challengeService;
        this.challengeScoringService = challengeScoringService;
        this.userRepository = userRepository;
        this.producerProfileRepository = producerProfileRepository;
    }

    @Transactional
    public List<ChallengeResult> close(Long challengeId, Long requestingUserId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (!requester.isAdmin()) {
            throw new ForbiddenOperationException("Solo un admin puede cerrar un challenge");
        }

        Challenge challenge = challengeService.getById(challengeId);
        List<Submission> submissions = submissionRepository.findByChallengeId(challengeId);

        Set<Long> verifiedProducerIds = producerProfileRepository.findByVerifiedTrue().stream()
                .map(ProducerProfile::getUser)
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Submission> ranked = submissions.stream()
                .sorted(Comparator.comparingDouble(
                        (Submission submission) -> scoreFor(challenge, verifiedProducerIds, submission)
                ).reversed())
                .limit(TOP_N)
                .toList();

        List<ChallengeResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Submission submission = ranked.get(i);
            int rank = i + 1;

            ChallengeResult result = new ChallengeResult();
            result.setChallengeId(challengeId);
            result.setSubmissionId(submission.getId());
            result.setRank(rank);
            result.setPointsAwarded(POINTS_BY_RANK[i]);
            result.setPrizeText(prizeFor(challenge, rank));
            if (rank == 1) {
                result.setBadge("Ganador del desafío");
                verifyWinner(submission.getProducerId());
            }
            results.add(challengeResultRepository.save(result));
        }
        return results;
    }

    private double scoreFor(Challenge challenge, Set<Long> verifiedProducerIds, Submission submission) {
        List<Vote> votes = voteRepository.findBySubmissionId(submission.getId());
        return challengeScoringService.computeScore(challenge.getGuestArtistId(), verifiedProducerIds, votes);
    }

    private String prizeFor(Challenge challenge, int rank) {
        return switch (rank) {
            case 1 -> challenge.getPrizeFirst();
            case 2 -> challenge.getPrizeSecond();
            case 3 -> challenge.getPrizeThird();
            default -> null;
        };
    }

    private void verifyWinner(Long producerId) {
        producerProfileRepository.findByUserId(producerId).ifPresent(profile -> {
            profile.setVerified(true);
            producerProfileRepository.save(profile);
        });
    }
}
```

- [ ] **Step 9: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ChallengeResultServiceTest`
Expected: PASS

- [ ] **Step 10: Escribir el test de controller e implementar `ChallengeCloseController`**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeCloseController.class)
class ChallengeCloseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeResultService challengeResultService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void closeReturns200WithResultsWhenRequesterIsAdmin() throws Exception {
        ChallengeResult result = new ChallengeResult();
        result.setRank(1);
        result.setPointsAwarded(500);

        when(challengeResultService.close(eq(100L), eq(1L))).thenReturn(java.util.List.of(result));

        mockMvc.perform(put("/api/challenges/100/close").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pointsAwarded").value(500));
    }

    @Test
    void closeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/challenges/100/close"))
                .andExpect(status().isUnauthorized());
    }
}
```

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChallengeCloseController {

    private final ChallengeResultService challengeResultService;

    public ChallengeCloseController(ChallengeResultService challengeResultService) {
        this.challengeResultService = challengeResultService;
    }

    @PutMapping("/api/challenges/{id}/close")
    public List<ChallengeResult> close(@PathVariable Long id,
                                        @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para cerrar un challenge");
        }
        return challengeResultService.close(id, requestingUserId);
    }
}
```

- [ ] **Step 11: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ChallengeCloseControllerTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/model/ChallengeResult.java \
        src/main/java/com/mgwprod/challenges/repository/ChallengeResultRepository.java \
        src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java \
        src/main/java/com/mgwprod/challenges/controller/ChallengeCloseController.java \
        src/test/java/com/mgwprod/challenges/repository/ChallengeResultRepositoryTest.java \
        src/test/java/com/mgwprod/challenges/service/ChallengeResultServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/ChallengeCloseControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(challenges): add close endpoint with top-3 results and auto-verification"
```

---

### Task 8: `PUT /api/challenges/{id}/opportunity-pick`

**Files:**
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeService.java`
- Modify: `src/main/java/com/mgwprod/challenges/controller/ChallengeController.java`
- Test: `src/test/java/com/mgwprod/challenges/service/ChallengeServiceTest.java` (agregar casos)
- Test: `src/test/java/com/mgwprod/challenges/controller/ChallengeControllerTest.java` (agregar casos)

**Interfaces:**
- Consumes: `SubmissionService.getById(Long)` (Task 4) para validar que la submission elegida pertenece al challenge.
- Produces: `ChallengeService.setOpportunityPick(Long challengeId, Long requestingUserId, Long submissionId)`.

- [ ] **Step 1: Agregar los tests a `ChallengeServiceTest`**

```java
    @Test
    void setOpportunityPickSucceedsWhenRequesterIsTheGuestArtist() {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(challenge));

        com.mgwprod.challenges.model.Submission submission = new com.mgwprod.challenges.model.Submission();
        submission.setId(7L);
        submission.setChallengeId(100L);
        when(submissionService.getById(7L)).thenReturn(submission);

        when(challengeRepository.save(challenge)).thenReturn(challenge);

        Challenge result = challengeService.setOpportunityPick(100L, 99L, 7L);

        assertThat(result.getOpportunityPickSubmissionId()).isEqualTo(7L);
    }

    @Test
    void setOpportunityPickThrowsWhenRequesterIsNotTheGuestArtist() {
        Challenge challenge = new Challenge();
        challenge.setId(100L);
        challenge.setGuestArtistId(99L);
        when(challengeRepository.findById(100L)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> challengeService.setOpportunityPick(100L, 1L, 7L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
```

Sumar el mock `@Mock private SubmissionService submissionService;` a la clase de test (con `@InjectMocks` Mockito lo inyecta solo).

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeServiceTest`
Expected: FAIL

- [ ] **Step 3: Implementar `ChallengeService.setOpportunityPick`**

Sumar a `ChallengeService.java` (agregar `SubmissionService` como dependencia en el constructor):

```java
    private final SubmissionService submissionService;

    // actualizar el constructor para recibir también SubmissionService:
    public ChallengeService(ChallengeRepository challengeRepository,
                             UserRepository userRepository,
                             SubmissionService submissionService) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
    }

    @Transactional
    public Challenge setOpportunityPick(Long challengeId, Long requestingUserId, Long submissionId) {
        Challenge challenge = getById(challengeId);
        if (!challenge.getGuestArtistId().equals(requestingUserId)) {
            throw new ForbiddenOperationException("Solo el artista invitado de este challenge puede elegir su opportunity pick");
        }
        Submission submission = submissionService.getById(submissionId);
        if (!submission.getChallengeId().equals(challengeId)) {
            throw new ForbiddenOperationException("La submission no pertenece a este challenge");
        }
        challenge.setOpportunityPickSubmissionId(submissionId);
        return challengeRepository.save(challenge);
    }
```

Ojo: como `ChallengeService` ahora depende de `SubmissionService` y `SubmissionService` ya depende de `ChallengeService` (Task 4), Spring arma una dependencia circular entre beans. Para resolverla sin reestructurar todo: en `SubmissionService`, cambiar el campo `ChallengeService` para que reciba `ChallengeRepository` directo en vez de `ChallengeService` (ya tenés `ChallengeRepository` inyectable), y llamar `challengeRepository.findById(challengeId).orElseThrow(() -> new ChallengeNotFoundException(challengeId))` en el lugar donde antes decía `challengeService.getById(challengeId)`. Ajustá también `SubmissionServiceTest` para mockear `ChallengeRepository` en vez de `ChallengeService`.

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ChallengeServiceTest,SubmissionServiceTest`
Expected: PASS

- [ ] **Step 5: Agregar el test y el endpoint al controller**

Sumar a `ChallengeControllerTest`:

```java
    @Test
    void opportunityPickReturns200WhenRequesterIsGuestArtist() throws Exception {
        Challenge response = new Challenge();
        response.setId(100L);
        response.setOpportunityPickSubmissionId(7L);

        when(challengeService.setOpportunityPick(eq(100L), eq(99L), eq(7L))).thenReturn(response);

        mockMvc.perform(put("/api/challenges/100/opportunity-pick")
                        .requestAttr("userId", 99L)
                        .param("submissionId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunityPickSubmissionId").value(7));
    }
```

Agregar el import estático `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;`.

Sumar a `ChallengeController.java`:

```java
    @PutMapping("/{id}/opportunity-pick")
    public Challenge opportunityPick(@PathVariable Long id,
                                      @RequestAttribute(name = "userId", required = false) Long userId,
                                      @RequestParam Long submissionId) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para esto");
        }
        return challengeService.setOpportunityPick(id, userId, submissionId);
    }
```

(agregar los imports `PutMapping` y `RequestParam` si faltan).

- [ ] **Step 6: Correr, verificar que pasa, y commit**

Run: `./mvnw test -Dtest=ChallengeControllerTest`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/service/ChallengeService.java \
        src/main/java/com/mgwprod/challenges/service/SubmissionService.java \
        src/main/java/com/mgwprod/challenges/controller/ChallengeController.java \
        src/test/java/com/mgwprod/challenges/service/ChallengeServiceTest.java \
        src/test/java/com/mgwprod/challenges/service/SubmissionServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/ChallengeControllerTest.java
git commit -m "feat(challenges): add opportunity-pick endpoint"
```

---

### Task 9: `GET /api/challenges/results` y `GET /api/ranking`

**Files:**
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java`
- Create: `src/main/java/com/mgwprod/challenges/model/RankingEntry.java`
- Create: `src/main/java/com/mgwprod/challenges/controller/RankingController.java`
- Modify: `src/main/java/com/mgwprod/challenges/controller/ChallengeCloseController.java` (o crear `ChallengeResultController` — ver Step 5)
- Test: `src/test/java/com/mgwprod/challenges/service/ChallengeResultServiceTest.java` (agregar casos)
- Test: `src/test/java/com/mgwprod/challenges/controller/RankingControllerTest.java`

**Interfaces:**
- Produces: `RankingEntry(Long producerId, int totalPoints)` (record — es un valor computado, no refleja ninguna entidad 1:1, por eso no cuenta como el DTO que se evita en el resto del proyecto), `ChallengeResultService.listResults(Long producerId)`, `.ranking()`.

- [ ] **Step 1: Agregar los tests a `ChallengeResultServiceTest`**

```java
    @Test
    void listResultsFiltersByProducerId() {
        com.mgwprod.challenges.model.Submission submission = new com.mgwprod.challenges.model.Submission();
        submission.setId(5L);
        submission.setProducerId(11L);
        when(submissionRepository.findByProducerId(11L)).thenReturn(List.of(submission));

        ChallengeResult result = new ChallengeResult();
        result.setSubmissionId(5L);
        when(challengeResultRepository.findBySubmissionIdIn(List.of(5L))).thenReturn(List.of(result));

        List<ChallengeResult> found = challengeResultService.listResults(11L);

        assertThat(found).hasSize(1);
    }

    @Test
    void rankingSumsPointsPerProducerDescending() {
        com.mgwprod.challenges.model.Submission submissionA = new com.mgwprod.challenges.model.Submission();
        submissionA.setId(1L);
        submissionA.setProducerId(11L);
        com.mgwprod.challenges.model.Submission submissionB = new com.mgwprod.challenges.model.Submission();
        submissionB.setId(2L);
        submissionB.setProducerId(12L);

        ChallengeResult resultA1 = new ChallengeResult();
        resultA1.setSubmissionId(1L);
        resultA1.setPointsAwarded(150);
        ChallengeResult resultB1 = new ChallengeResult();
        resultB1.setSubmissionId(2L);
        resultB1.setPointsAwarded(500);

        when(challengeResultRepository.findAll()).thenReturn(List.of(resultA1, resultB1));
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submissionA));
        when(submissionRepository.findById(2L)).thenReturn(Optional.of(submissionB));

        List<com.mgwprod.challenges.model.RankingEntry> ranking = challengeResultService.ranking();

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).producerId()).isEqualTo(12L);
        assertThat(ranking.get(0).totalPoints()).isEqualTo(500);
        assertThat(ranking.get(1).producerId()).isEqualTo(11L);
        assertThat(ranking.get(1).totalPoints()).isEqualTo(150);
    }
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ChallengeResultServiceTest`
Expected: FAIL

- [ ] **Step 3: Crear `RankingEntry` e implementar los métodos**

```java
package com.mgwprod.challenges.model;

public record RankingEntry(Long producerId, int totalPoints) {
}
```

Sumar a `ChallengeResultService.java`:

```java
    @Transactional(readOnly = true)
    public List<ChallengeResult> listResults(Long producerId) {
        if (producerId != null) {
            List<Long> submissionIds = submissionRepository.findByProducerId(producerId).stream()
                    .map(Submission::getId)
                    .toList();
            return challengeResultRepository.findBySubmissionIdIn(submissionIds);
        }
        return challengeResultRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<com.mgwprod.challenges.model.RankingEntry> ranking() {
        java.util.Map<Long, Integer> pointsByProducer = new java.util.HashMap<>();
        for (ChallengeResult result : challengeResultRepository.findAll()) {
            Submission submission = submissionRepository.findById(result.getSubmissionId())
                    .orElseThrow(() -> new com.mgwprod.challenges.exception.SubmissionNotFoundException(result.getSubmissionId()));
            pointsByProducer.merge(submission.getProducerId(), result.getPointsAwarded(), Integer::sum);
        }
        return pointsByProducer.entrySet().stream()
                .map(entry -> new com.mgwprod.challenges.model.RankingEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(com.mgwprod.challenges.model.RankingEntry::totalPoints).reversed())
                .toList();
    }
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ChallengeResultServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller e implementar los endpoints**

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.RankingEntry;
import com.mgwprod.challenges.service.ChallengeResultService;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeResultService challengeResultService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void rankingReturns200SortedByPoints() throws Exception {
        when(challengeResultService.ranking()).thenReturn(java.util.List.of(new RankingEntry(12L, 500)));

        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].producerId").value(12))
                .andExpect(jsonPath("$[0].totalPoints").value(500));
    }

    @Test
    void resultsReturns200FilteredByProducerId() throws Exception {
        when(challengeResultService.listResults(11L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/challenges/results").param("producerId", "11"))
                .andExpect(status().isOk());
    }
}
```

```java
package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.RankingEntry;
import com.mgwprod.challenges.service.ChallengeResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RankingController {

    private final ChallengeResultService challengeResultService;

    public RankingController(ChallengeResultService challengeResultService) {
        this.challengeResultService = challengeResultService;
    }

    @GetMapping("/api/ranking")
    public List<RankingEntry> ranking() {
        return challengeResultService.ranking();
    }

    @GetMapping("/api/challenges/results")
    public List<ChallengeResult> results(@RequestParam(required = false) Long producerId) {
        return challengeResultService.listResults(producerId);
    }
}
```

- [ ] **Step 6: Correr toda la suite del módulo, verificar que pasa, y commit**

Run: `./mvnw test -Dtest="com.mgwprod.challenges.**"`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/challenges/model/RankingEntry.java \
        src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java \
        src/main/java/com/mgwprod/challenges/controller/RankingController.java \
        src/test/java/com/mgwprod/challenges/service/ChallengeResultServiceTest.java \
        src/test/java/com/mgwprod/challenges/controller/RankingControllerTest.java
git commit -m "feat(challenges): add results and ranking endpoints"
```

---

## Al terminar

1. Correr `./mvnw test` completo para confirmar que no rompiste nada de `users`.
2. Armar la colección de Postman/`.http` de los 9 endpoints (incluida la verificación de productores).
3. Escribir `docs/api/challenges-API.md`.
4. Abrir un PR de `feature/challenges-module` contra `main`.

## Nota sobre paralelismo con `catalog`/`collab`

Este plan no depende de `catalog` ni de `collab` (solo de `users`) — se puede ejecutar en paralelo con esos dos. La única coordinación necesaria es en `docs/db/schema.sql`: si los tres PRs tocan el archivo casi al mismo tiempo, van a pisarse al mergear. Acordá con Santiago y Dani un orden de merge, y quien mergee después resuelve el conflicto de `schema.sql` a mano (es solo agregar los `CREATE TABLE`/`ALTER TABLE` de todos, no hay lógica que resolver).
