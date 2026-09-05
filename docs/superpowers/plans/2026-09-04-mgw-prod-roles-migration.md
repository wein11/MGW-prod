# Migración de roles (ARTIST/DISCOGRAFICA/ADMIN) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrar `Role` de `{PRODUCER, ARTIST}` a `{ARTIST, DISCOGRAFICA, ADMIN}`, eliminar el flag `users.is_admin`, y fusionar `producer_profiles`+`artist_profiles` en una sola tabla `artist_profiles`. Es la base de la que dependen los otros dos correcciones del profesor (CRUD completo y billing) — se ejecuta y mergea primero. Dueño: Santiago.

**Architecture:** Sin paquetes nuevos — todos los cambios son dentro de `com.mgwprod.users` (modelo/servicio/controller/repositorio) más ajustes puntuales de autorización en `catalog`/`challenges` donde hoy se chequea `Role.PRODUCER` o `User.isAdmin()`.

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-04-mgw-prod-professor-corrections-design.md` (sección 1).

## Global Constraints

- Mismas convenciones ya vigentes: sin DTOs, validar solo con `@Valid @RequestBody` en el controller, `ddl-auto=none` con schema manual, auth casera vía `SessionAuthInterceptor`.
- **No se migran datos**: se reescribe `docs/db/schema.sql` completo y se recrea la base local (`DROP DATABASE mgw_prod; CREATE DATABASE mgw_prod;` seguido de `mysql -u root -padmin mgw_prod < docs/db/schema.sql`) — es todo dato de smoke test.
- Rama nueva desde `main` actualizado: `git checkout main && git pull && git checkout -b feature/roles-migration`.
- Al terminar, avisar a Paolo y Dani cuando esta branch esté mergeada a `main` — sus planes (`2026-09-04-mgw-prod-crud-completion.md`, `2026-09-04-mgw-prod-billing-module.md`) asumen el `Role` nuevo y no compilan contra el viejo.

---

### Task 1: `Role` enum + `User` (quitar `isAdmin`)

**Files:**
- Modify: `src/main/java/com/mgwprod/users/model/Role.java`
- Modify: `src/main/java/com/mgwprod/users/model/User.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/users/model/UserJsonTest.java`

- [ ] **Step 1: Actualizar el test que hoy asume `isAdmin`**

`UserJsonTest` hoy verifica que `isAdmin` serializa/deserializa como boolean. Reemplazar esas aserciones por chequear que `role` acepta `"ADMIN"` como cualquier otro valor del enum (ya no hay campo separado). Correr `./mvnw test -Dtest=UserJsonTest` y confirmar que falla mientras el enum viejo siga en pie (el test nuevo espera `Role.ADMIN`, que todavía no existe).

- [ ] **Step 2: `Role.java`**

```java
package com.mgwprod.users.model;

public enum Role {
    ARTIST,
    DISCOGRAFICA,
    ADMIN
}
```

- [ ] **Step 3: `User.java` — quitar el campo `admin`/`isAdmin`**

Borrar el bloque completo del campo `admin` (columna `is_admin`, `@JsonProperty("isAdmin")` y el comentario que lo explica) de `User.java`. El resto de la entidad queda igual.

- [ ] **Step 4: Schema — quitar `is_admin`**

En `docs/db/schema.sql`, quitar la línea `is_admin BOOLEAN NOT NULL DEFAULT FALSE,` de `CREATE TABLE users`. El tipo de `role VARCHAR(20)` no cambia — ya admite cualquier string, incluido `DISCOGRAFICA` (12 caracteres).

- [ ] **Step 5: Correr `UserJsonTest`, confirmar que pasa**

Run: `./mvnw test -Dtest=UserJsonTest`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mgwprod/users/model/Role.java \
        src/main/java/com/mgwprod/users/model/User.java \
        src/test/java/com/mgwprod/users/model/UserJsonTest.java \
        docs/db/schema.sql
git commit -m "feat(users): migrate Role to ARTIST/DISCOGRAFICA/ADMIN, drop is_admin flag"
```

Nota: este commit deja el proyecto sin compilar (todo lo que referencia `Role.PRODUCER`/`isAdmin` en otros archivos todavía no se tocó) — es esperable, se resuelve en las tareas siguientes. No hacer `./mvnw test` del proyecto completo hasta el final de la Task 7.

---

### Task 2: Fusionar `ProducerProfile` en `ArtistProfile`

**Files:**
- Modify: `src/main/java/com/mgwprod/users/model/ArtistProfile.java`
- Delete: `src/main/java/com/mgwprod/users/model/ProducerProfile.java`
- Modify: `src/main/java/com/mgwprod/users/repository/ArtistProfileRepository.java`
- Delete: `src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/users/model/ProfileJsonTest.java`

- [ ] **Step 1: `ArtistProfile.java` — unión de campos**

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

    @Min(value = 1, message = "El BPM mínimo debe ser mayor a 0")
    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Min(value = 1, message = "El BPM máximo debe ser mayor a 0")
    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Size(min = 1, message = "El nivel de experiencia no puede estar vacío")
    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(nullable = false)
    private boolean verified = false;
}
```

- [ ] **Step 2: Borrar `ProducerProfile.java`**

- [ ] **Step 3: `ArtistProfileRepository.java` — sumar `findByVerifiedTrue`**

```java
package com.mgwprod.users.repository;

import com.mgwprod.users.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    Optional<ArtistProfile> findByUserId(Long userId);

    List<ArtistProfile> findByVerifiedTrue();
}
```

- [ ] **Step 4: Borrar `ProducerProfileRepository.java`**

- [ ] **Step 5: Schema — fusionar tablas**

En `docs/db/schema.sql`, borrar por completo el `CREATE TABLE producer_profiles` y la línea `ALTER TABLE producer_profiles ADD COLUMN verified ...`. Reemplazar `CREATE TABLE artist_profiles` por:

```sql
CREATE TABLE artist_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    genres VARCHAR(255),
    bio TEXT,
    bpm_min INT,
    bpm_max INT,
    experience_level VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

- [ ] **Step 6: Actualizar `ProfileJsonTest`**

Quitar cualquier caso que instancie `ProducerProfile`; sumar un caso que confirme que `ArtistProfile` serializa/deserializa `bpmMin`/`bpmMax`/`experienceLevel`/`verified` además de `genres`/`bio`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/mgwprod/users/model/ArtistProfile.java \
        src/main/java/com/mgwprod/users/repository/ArtistProfileRepository.java \
        src/test/java/com/mgwprod/users/model/ProfileJsonTest.java \
        docs/db/schema.sql
git rm src/main/java/com/mgwprod/users/model/ProducerProfile.java \
       src/main/java/com/mgwprod/users/repository/ProducerProfileRepository.java
git commit -m "feat(users): merge ProducerProfile fields into ArtistProfile"
```

---

### Task 3: `AuthService.register` — un solo perfil, solo para `ARTIST`

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/AuthService.java`
- Modify: `src/test/java/com/mgwprod/users/service/AuthServiceTest.java`

- [ ] **Step 1: Actualizar los tests que registran un `PRODUCER`**

En `AuthServiceTest`, todo caso que registraba un `User` con `role=PRODUCER` y esperaba un `ProducerProfile` pasa a registrar `role=ARTIST` y esperar un `ArtistProfile` (mismo repositorio para ambos casos ahora). Sumar (o confirmar que ya existe) un caso que registra `role=DISCOGRAFICA` y verifica que **no** se crea ningún `ArtistProfile`.

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=AuthServiceTest`

- [ ] **Step 3: Reescribir `register`**

```java
package com.mgwprod.users.service;

import com.mgwprod.users.exception.EmailAlreadyExistsException;
import com.mgwprod.users.exception.InvalidCredentialsException;
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

    @Transactional
    public User register(User incoming) {
        if (userRepository.existsByEmail(incoming.getEmail())) {
            throw new EmailAlreadyExistsException(incoming.getEmail());
        }

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
}
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=AuthServiceTest`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/service/AuthService.java \
        src/test/java/com/mgwprod/users/service/AuthServiceTest.java
git commit -m "feat(users): only create ArtistProfile for role=ARTIST on register"
```

---

### Task 4: `UserService` — perfil fusionado + `verifyArtist`

**Files:**
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java`
- Modify: `src/test/java/com/mgwprod/users/service/UserServiceTest.java`

- [ ] **Step 1: Actualizar `UserServiceTest`**

- Todo caso de `updateProducerProfile` se borra; sus asserts (genres/bpm/experience) se mueven a nuevos casos de `updateArtistProfile` (ahora acepta esos campos también).
- `getProfile` para un usuario `DISCOGRAFICA`: decidir que devuelve `404`/excepción clara (`ArtistProfileNotApplicableException` o similar) en vez de intentar buscar un perfil que no existe — sumar un test para ese caso.
- `verifyProducer` → `verifyArtist`: mismo comportamiento (solo admin, falla si el target no es `ARTIST`), pero usando `Role.ADMIN` en vez de `isAdmin()` y `ArtistProfileRepository` en vez de `ProducerProfileRepository`.

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=UserServiceTest`

- [ ] **Step 3: Reescribir `UserService`**

```java
package com.mgwprod.users.service;

import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public UserService(UserRepository userRepository, ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public ArtistProfile getProfile(Long userId) {
        User user = getById(userId);
        if (user.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de artista");
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
        if (request.getBpmMin() != null) {
            profile.setBpmMin(request.getBpmMin());
        }
        if (request.getBpmMax() != null) {
            profile.setBpmMax(request.getBpmMax());
        }
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(request.getExperienceLevel());
        }
        return artistProfileRepository.save(profile);
    }

    @Transactional
    public ArtistProfile verifyArtist(Long requestingUserId, Long artistId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo un admin puede verificar artistas");
        }
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new UserNotFoundException(artistId));
        if (artist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo se puede verificar a un artista");
        }
        ArtistProfile profile = artistProfileRepository.findByUserId(artistId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + artistId));
        profile.setVerified(true);
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

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=UserServiceTest`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/users/service/UserService.java \
        src/test/java/com/mgwprod/users/service/UserServiceTest.java
git commit -m "feat(users): merge profile update into ArtistProfile, rename verifyProducer to verifyArtist"
```

---

### Task 5: Controllers — `UserController` y `ArtistVerificationController`

**Files:**
- Modify: `src/main/java/com/mgwprod/users/controller/UserController.java`
- Rename+Modify: `src/main/java/com/mgwprod/users/controller/ProducerVerificationController.java` → `ArtistVerificationController.java`
- Modify: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`
- Rename+Modify: `src/test/java/com/mgwprod/users/controller/ProducerVerificationControllerTest.java` → `ArtistVerificationControllerTest.java`

- [ ] **Step 1: Actualizar los tests**

- `UserControllerTest`: borrar el caso de `PUT /api/users/{id}/producer-profile`; el caso de `artist-profile` ahora manda/espera también `bpmMin`/`bpmMax`/`experienceLevel`.
- Renombrar el archivo de test del controller de verificación y su clase a `ArtistVerificationControllerTest`, apuntando a `PUT /api/artists/{id}/verify`.

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=UserControllerTest,ArtistVerificationControllerTest`

- [ ] **Step 3: `UserController.java` — quitar el endpoint de producer-profile**

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.ArtistProfile;
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
    public ArtistProfile getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id,
                            @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                            @RequestBody User request) {
        requireAuthenticated(requestingUserId);
        return userService.updateUser(id, requestingUserId, request);
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

- [ ] **Step 4: `ArtistVerificationController.java`** (reemplaza a `ProducerVerificationController.java`)

```java
package com.mgwprod.users.controller;

import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.service.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArtistVerificationController {

    private final UserService userService;

    public ArtistVerificationController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/api/artists/{id}/verify")
    public ArtistProfile verify(@PathVariable Long id,
                                 @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para verificar un artista");
        }
        return userService.verifyArtist(requestingUserId, id);
    }
}
```

- [ ] **Step 5: Borrar el archivo viejo, correr los tests, confirmar que pasan**

```bash
git rm src/main/java/com/mgwprod/users/controller/ProducerVerificationController.java
```

Run: `./mvnw test -Dtest=UserControllerTest,ArtistVerificationControllerTest`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mgwprod/users/controller/UserController.java \
        src/main/java/com/mgwprod/users/controller/ArtistVerificationController.java \
        src/test/java/com/mgwprod/users/controller/UserControllerTest.java \
        src/test/java/com/mgwprod/users/controller/ArtistVerificationControllerTest.java
git commit -m "feat(users): rename producer verification to artist verification, drop producer-profile endpoint"
```

---

### Task 6: Autorización en `catalog` y `challenges`

**Files:**
- Modify: `src/main/java/com/mgwprod/catalog/service/BeatService.java`
- Modify: `src/main/java/com/mgwprod/challenges/service/SubmissionService.java`
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeService.java`
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java`
- Modify: tests de los 4 anteriores + `src/test/java/com/mgwprod/collab/repository/CommentRepositoryTest.java`, `src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java` (solo cambian los datos de prueba: cualquier `User` con `role=PRODUCER` pasa a `role=ARTIST`; no hay lógica que tocar en `collab`, ya exigía `ARTIST`)

- [ ] **Step 1: Actualizar los tests con las nuevas expectativas**

- `BeatServiceTest`/`BeatControllerTest`: el caso "solo productor puede publicar" pasa a probar con dos artistas cualesquiera (ya no hay distinción) más un caso nuevo: un `DISCOGRAFICA` intentando publicar un beat recibe 403.
- `SubmissionServiceTest`/`ChallengeServiceTest` (submissions): mismo cambio — `role=ARTIST` en vez de `PRODUCER`, más el caso `DISCOGRAFICA` → 403.
- `ChallengeServiceTest` (create): sumar un caso donde `role=DISCOGRAFICA` **sí** puede crear el challenge; el caso existente de "no admin" ahora prueba con `role=ARTIST` (que tampoco puede).
- `ChallengeResultServiceTest`: los mocks de `ProducerProfileRepository` pasan a `ArtistProfileRepository`; `verifiedProducerIds`/`verifyWinner` deben seguir funcionando igual, solo cambia el repositorio de origen.

- [ ] **Step 2: Correr toda la suite, confirmar los fallos esperados**

Run: `./mvnw test -Dtest="com.mgwprod.catalog.**,com.mgwprod.challenges.**"`

- [ ] **Step 3: `BeatService.create` — `ARTIST` en vez de `PRODUCER`**

```java
    @Transactional
    public Beat create(Long producerId, Beat beat) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede publicar beats");
        }
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }
```

- [ ] **Step 4: `SubmissionService.create` — `ARTIST` en vez de `PRODUCER`**

```java
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede enviar una submission");
        }
```

- [ ] **Step 5: `ChallengeService.create` — admin o discográfica**

```java
    @Transactional
    public Challenge create(Long requestingUserId, Challenge challenge) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.DISCOGRAFICA) {
            throw new ForbiddenOperationException("Solo un admin o una discográfica pueden crear challenges");
        }
        User guestArtist = userRepository.findById(challenge.getGuestArtistId())
                .orElseThrow(() -> new UserNotFoundException(challenge.getGuestArtistId()));
        if (guestArtist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("El artista invitado debe tener rol ARTIST");
        }
        return challengeRepository.save(challenge);
    }
```

- [ ] **Step 6: `ChallengeResultService` — `ArtistProfileRepository` en vez de `ProducerProfileRepository`, `Role.ADMIN` en vez de `isAdmin()`**

Cambiar el tipo del campo/constructor inyectado de `ProducerProfileRepository` a `ArtistProfileRepository`, y:

```java
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo un admin puede cerrar un challenge");
        }
```
(agregar `import com.mgwprod.users.model.Role;`)

```java
        Set<Long> verifiedProducerIds = artistProfileRepository.findByVerifiedTrue().stream()
                .map(ArtistProfile::getUser)
                .map(User::getId)
                .collect(Collectors.toSet());
```
(cambiar el import de `ProducerProfile` a `ArtistProfile`)

```java
    private void verifyWinner(Long producerId) {
        artistProfileRepository.findByUserId(producerId).ifPresent(profile -> {
            profile.setVerified(true);
            artistProfileRepository.save(profile);
        });
    }
```

Renombrar el campo/parámetro `producerProfileRepository` → `artistProfileRepository` en todo el archivo (constructor incluido).

- [ ] **Step 7: Correr toda la suite, confirmar que pasa**

Run: `./mvnw test -Dtest="com.mgwprod.catalog.**,com.mgwprod.challenges.**,com.mgwprod.collab.**"`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mgwprod/catalog/service/BeatService.java \
        src/main/java/com/mgwprod/challenges/service/SubmissionService.java \
        src/main/java/com/mgwprod/challenges/service/ChallengeService.java \
        src/main/java/com/mgwprod/challenges/service/ChallengeResultService.java \
        src/test/java/com/mgwprod/catalog \
        src/test/java/com/mgwprod/challenges \
        src/test/java/com/mgwprod/collab
git commit -m "feat: update authorization checks for ARTIST/DISCOGRAFICA/ADMIN roles"
```

---

### Task 7: Suite completa, docs, recreación de la base local

- [ ] **Step 1: Correr toda la suite del proyecto**

Run: `./mvnw test`
Expected: PASS (0 referencias colgantes a `Role.PRODUCER`, `isAdmin`, `ProducerProfile`)

- [ ] **Step 2: Recrear la base local**

```bash
mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

- [ ] **Step 3: Smoke test manual mínimo**

Levantar la app y probar a mano (curl/Postman): registrar un `ARTIST`, un `DISCOGRAFICA` y un `ADMIN` (vía SQL directo para el primer admin, no hay endpoint público — igual que antes con `is_admin`); login de los 3; `POST /api/beats` con el `ARTIST` (201) y con el `DISCOGRAFICA` (403); `POST /api/challenges` con el `DISCOGRAFICA` (201); `PUT /api/artists/{id}/verify` con el `ADMIN` (200).

- [ ] **Step 4: Actualizar `docs/api/users-API.md`**

- Reemplazar toda mención de `PRODUCER`/`isAdmin` por `ARTIST`/`DISCOGRAFICA`/`ADMIN`.
- Renombrar la sección del endpoint de verificación a `PUT /api/artists/{id}/verify`.
- Quitar la sección de `PUT /api/users/{id}/producer-profile`; documentar que `artist-profile` ahora acepta también `bpmMin`/`bpmMax`/`experienceLevel`.

- [ ] **Step 5: Commit final y push**

```bash
git add docs/api/users-API.md
git commit -m "docs(users): update users-API.md for the new role model"
git push -u origin feature/roles-migration
```

## Al terminar

1. Abrir PR de `feature/roles-migration` contra `main`.
2. Una vez mergeado, avisar a Paolo y Dani — sus planes parten de `main` ya con este cambio adentro.
