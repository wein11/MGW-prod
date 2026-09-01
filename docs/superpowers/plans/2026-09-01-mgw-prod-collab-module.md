# Módulo `collab` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el módulo `collab` (toplines de artistas sobre beats, comentarios, colaboraciones) del pivot de mgw-prod, dueño: Dani. Reemplaza al viejo módulo `orders` (carrito/checkout, nunca implementado en código — solo existía en specs viejas).

**Architecture:** Paquete vertical `com.mgwprod.collab` (controller/service/repository/model/exception), mismo patrón que `users`/`catalog`: sin DTOs, Service con la lógica, Repository `JpaRepository`. `collab` referencia tanto `User` (de `users`) como `Beat` (de `catalog`) por FK plana (`Long`), nunca al revés — ni `users` ni `catalog` conocen `collab`.

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` (secciones "Módulo `collab`" y "Endpoints").

## Global Constraints

- **Sin DTOs**: los controllers reciben y devuelven las entidades JPA (`Topline`, `Comment`, `Collaboration`) directamente.
- **Validación solo en el controller**: usar siempre `@Valid @RequestBody`; Hibernate no valida al hacer `save()` (`jakarta.persistence.validation.mode=none` a nivel app).
- **Esquema manual**: `ddl-auto=none`. Sumar las tablas nuevas al final de `docs/db/schema.sql` y correr el script contra MySQL local.
- **Auth casera**: `SessionAuthInterceptor` setea `userId`/`userRole` como request attributes cuando hay un Bearer token válido; si no hay token, el request pasa sin esos attributes y cada controller decide si rechaza.
- **Nunca commitear a `main` directo** — trabajo en una branch nueva (ver Prerequisito).
- **Dependencia solo hacia `users` y `catalog`**: usar `com.mgwprod.users.repository.UserRepository` + `com.mgwprod.users.model.{User,Role}` para validar artistId/producerId, y `com.mgwprod.catalog.repository.BeatRepository` + `com.mgwprod.catalog.model.Beat` para validar beatId. No importar nada de `collab` desde `users` ni `catalog` — la dependencia va en un solo sentido.

## Prerequisito (una sola vez, antes de la Task 1)

Este plan depende de que **el PR de `catalog`** (Santiago + Mateo) ya esté mergeado a `main` — `collab` necesita `BeatRepository`/`Beat` para validar el `beatId` de cada Topline. Si `catalog` todavía no está mergeado, coordiná con Santiago antes de arrancar (o, si el grupo prefiere avanzar en paralelo, decidan juntos si mockear temporalmente esa dependencia). El PR de "sin DTOs" (`refactor/users-remove-dtos`, PR #1) ya está mergeado a `main` (2026-09-01), así que ese prerequisito ya está resuelto.

```bash
git checkout main
git pull
git checkout -b feature/collab-module
```

---

### Task 1: Entidad `Topline` + repositorio + schema

**Files:**
- Create: `src/main/java/com/mgwprod/collab/model/Topline.java`
- Create: `src/main/java/com/mgwprod/collab/repository/ToplineRepository.java`
- Create: `src/main/java/com/mgwprod/collab/exception/ToplineNotFoundException.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/collab/repository/ToplineRepositoryTest.java`

**Interfaces:**
- Produces: `Topline` (id, artistId, beatId, audioUrl, createdAt), `ToplineRepository.findByBeatId(Long)`, `.findByArtistId(Long)`, `ToplineNotFoundException(Long)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE toplines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    beat_id BIGINT NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (artist_id) REFERENCES users(id),
    FOREIGN KEY (beat_id) REFERENCES beats(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Topline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class ToplineRepositoryTest {

    @Autowired
    private ToplineRepository toplineRepository;

    @Test
    void findByBeatIdReturnsOnlyToplinesForThatBeat() {
        Topline topline = new Topline();
        topline.setArtistId(1L);
        topline.setBeatId(2L);
        topline.setAudioUrl("https://soundcloud.com/example/topline");
        toplineRepository.save(topline);

        List<Topline> result = toplineRepository.findByBeatId(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ToplineRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear la entidad**

```java
package com.mgwprod.collab.model;

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
@Table(name = "toplines")
@Getter
@Setter
@NoArgsConstructor
public class Topline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El artista es obligatorio")
    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @NotNull(message = "El beat es obligatorio")
    @Column(name = "beat_id", nullable = false)
    private Long beatId;

    @NotBlank(message = "El link de audio es obligatorio")
    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

- [ ] **Step 5: Crear el repositorio**

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Topline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToplineRepository extends JpaRepository<Topline, Long> {
    List<Topline> findByBeatId(Long beatId);
    List<Topline> findByArtistId(Long artistId);
}
```

- [ ] **Step 6: Crear la excepción**

```java
package com.mgwprod.collab.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ToplineNotFoundException extends ApiException {
    public ToplineNotFoundException(Long toplineId) {
        super(HttpStatus.NOT_FOUND, "No existe un topline con id: " + toplineId);
    }
}
```

- [ ] **Step 7: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ToplineRepositoryTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mgwprod/collab/model/Topline.java \
        src/main/java/com/mgwprod/collab/repository/ToplineRepository.java \
        src/main/java/com/mgwprod/collab/exception/ToplineNotFoundException.java \
        src/test/java/com/mgwprod/collab/repository/ToplineRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(collab): add Topline entity, repository, and schema"
```

---

### Task 2: Entidad `Collaboration` + repositorio + schema

La `Collaboration` se crea automáticamente en `PENDING` cada vez que se sube un `Topline` (ver Task 3) — por eso conviene tener la entidad lista antes de escribir `ToplineService.create`.

**Files:**
- Create: `src/main/java/com/mgwprod/collab/model/Collaboration.java`
- Create: `src/main/java/com/mgwprod/collab/model/CollaborationStatus.java`
- Create: `src/main/java/com/mgwprod/collab/repository/CollaborationRepository.java`
- Create: `src/main/java/com/mgwprod/collab/exception/CollaborationNotFoundException.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/collab/repository/CollaborationRepositoryTest.java`

**Interfaces:**
- Produces: `Collaboration` (id, toplineId, status, decidedAt), `CollaborationStatus` enum (`PENDING`, `ACCEPTED`, `REJECTED`), `CollaborationRepository.findByToplineId(Long)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE collaborations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topline_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    decided_at DATETIME,
    FOREIGN KEY (topline_id) REFERENCES toplines(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class CollaborationRepositoryTest {

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Test
    void findByToplineIdReturnsTheCollaboration() {
        Collaboration collaboration = new Collaboration();
        collaboration.setToplineId(1L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaborationRepository.save(collaboration);

        Optional<Collaboration> result = collaborationRepository.findByToplineId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(CollaborationStatus.PENDING);
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=CollaborationRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear el enum de estado**

```java
package com.mgwprod.collab.model;

public enum CollaborationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
```

- [ ] **Step 5: Crear la entidad**

```java
package com.mgwprod.collab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "collaborations")
@Getter
@Setter
@NoArgsConstructor
public class Collaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El topline es obligatorio")
    @Column(name = "topline_id", nullable = false, unique = true)
    private Long toplineId;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationStatus status;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
```

- [ ] **Step 6: Crear repositorio y excepción**

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Collaboration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {
    Optional<Collaboration> findByToplineId(Long toplineId);
    List<Collaboration> findByStatus(com.mgwprod.collab.model.CollaborationStatus status);
}
```

```java
package com.mgwprod.collab.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CollaborationNotFoundException extends ApiException {
    public CollaborationNotFoundException(Long collaborationId) {
        super(HttpStatus.NOT_FOUND, "No existe una colaboración con id: " + collaborationId);
    }
}
```

- [ ] **Step 7: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CollaborationRepositoryTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mgwprod/collab/model/Collaboration.java \
        src/main/java/com/mgwprod/collab/model/CollaborationStatus.java \
        src/main/java/com/mgwprod/collab/repository/CollaborationRepository.java \
        src/main/java/com/mgwprod/collab/exception/CollaborationNotFoundException.java \
        src/test/java/com/mgwprod/collab/repository/CollaborationRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(collab): add Collaboration entity, repository, and schema"
```

---

### Task 3: `POST /api/toplines` — sube el topline y crea la Collaboration en PENDING

**Files:**
- Create: `src/main/java/com/mgwprod/collab/service/ToplineService.java`
- Create: `src/main/java/com/mgwprod/collab/controller/ToplineController.java`
- Test: `src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java`
- Test: `src/test/java/com/mgwprod/collab/controller/ToplineControllerTest.java`

**Interfaces:**
- Consumes: `Topline`/`ToplineRepository` (Task 1), `Collaboration`/`CollaborationStatus`/`CollaborationRepository` (Task 2), `com.mgwprod.catalog.repository.BeatRepository` + `com.mgwprod.catalog.exception.BeatNotFoundException` (módulo `catalog`), `com.mgwprod.users.repository.UserRepository` + `com.mgwprod.users.model.{User,Role}` + `com.mgwprod.users.exception.{UserNotFoundException, ForbiddenOperationException, UnauthenticatedException}`.
- Produces: `ToplineService.create(Long artistId, Topline topline)` — internamente crea también la `Collaboration`.

- [ ] **Step 1: Escribir el test de servicio**

```java
package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToplineServiceTest {

    @Mock
    private ToplineRepository toplineRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BeatRepository beatRepository;

    @InjectMocks
    private ToplineService toplineService;

    @Test
    void createSavesToplineAndPendingCollaboration() {
        User artist = new User();
        artist.setId(1L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setId(2L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        Topline topline = new Topline();
        topline.setBeatId(2L);
        topline.setAudioUrl("https://soundcloud.com/example/topline");

        when(toplineRepository.save(any(Topline.class))).thenAnswer(invocation -> {
            Topline saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(collaborationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Topline saved = toplineService.create(1L, topline);

        assertThat(saved.getArtistId()).isEqualTo(1L);

        ArgumentCaptor<com.mgwprod.collab.model.Collaboration> captor =
                ArgumentCaptor.forClass(com.mgwprod.collab.model.Collaboration.class);
        verify(collaborationRepository).save(captor.capture());
        assertThat(captor.getValue().getToplineId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(CollaborationStatus.PENDING);
    }

    @Test
    void createThrowsWhenUserIsNotArtist() {
        User producer = new User();
        producer.setId(1L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(producer));

        Topline topline = new Topline();
        topline.setBeatId(2L);

        assertThatThrownBy(() -> toplineService.create(1L, topline))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ToplineServiceTest`
Expected: FAIL

- [ ] **Step 3: Implementar `ToplineService`**

```java
package com.mgwprod.collab.service;

import com.mgwprod.catalog.exception.BeatNotFoundException;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.exception.ToplineNotFoundException;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ToplineService {

    private final ToplineRepository toplineRepository;
    private final CollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final BeatRepository beatRepository;

    public ToplineService(ToplineRepository toplineRepository,
                           CollaborationRepository collaborationRepository,
                           UserRepository userRepository,
                           BeatRepository beatRepository) {
        this.toplineRepository = toplineRepository;
        this.collaborationRepository = collaborationRepository;
        this.userRepository = userRepository;
        this.beatRepository = beatRepository;
    }

    @Transactional
    public Topline create(Long artistId, Topline topline) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new UserNotFoundException(artistId));
        if (artist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede subir un topline");
        }
        beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new BeatNotFoundException(topline.getBeatId()));

        topline.setArtistId(artistId);
        Topline saved = toplineRepository.save(topline);

        Collaboration collaboration = new Collaboration();
        collaboration.setToplineId(saved.getId());
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaborationRepository.save(collaboration);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Topline> list(Long beatId, Long artistId) {
        if (beatId != null) {
            return toplineRepository.findByBeatId(beatId);
        }
        if (artistId != null) {
            return toplineRepository.findByArtistId(artistId);
        }
        return toplineRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Topline getById(Long id) {
        return toplineRepository.findById(id)
                .orElseThrow(() -> new ToplineNotFoundException(id));
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ToplineServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller**

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.service.ToplineService;
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

@WebMvcTest(ToplineController.class)
class ToplineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ToplineService toplineService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createToplineReturns201WhenAuthenticated() throws Exception {
        Topline request = new Topline();
        request.setBeatId(2L);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        Topline response = new Topline();
        response.setId(10L);
        response.setArtistId(1L);
        response.setBeatId(2L);

        when(toplineService.create(eq(1L), any(Topline.class))).thenReturn(response);

        mockMvc.perform(post("/api/toplines")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beatId").value(2));
    }

    @Test
    void createToplineReturns401WhenNotAuthenticated() throws Exception {
        Topline request = new Topline();
        request.setBeatId(2L);
        request.setAudioUrl("https://soundcloud.com/example/topline");

        mockMvc.perform(post("/api/toplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listToplinesReturns200() throws Exception {
        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);

        when(toplineService.list(2L, null)).thenReturn(java.util.List.of(topline));

        mockMvc.perform(get("/api/toplines").param("beatId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beatId").value(2));
    }
}
```

- [ ] **Step 6: Correr, verificar que falla**

Run: `./mvnw test -Dtest=ToplineControllerTest`
Expected: FAIL

- [ ] **Step 7: Implementar `ToplineController`**

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.service.ToplineService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/toplines")
public class ToplineController {

    private final ToplineService toplineService;

    public ToplineController(ToplineService toplineService) {
        this.toplineService = toplineService;
    }

    @PostMapping
    public ResponseEntity<Topline> createTopline(@RequestAttribute(name = "userId", required = false) Long userId,
                                                  @Valid @RequestBody Topline topline) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para subir un topline");
        }
        Topline created = toplineService.create(userId, topline);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Topline> listToplines(@RequestParam(required = false) Long beatId,
                                       @RequestParam(required = false) Long artistId) {
        return toplineService.list(beatId, artistId);
    }

    @GetMapping("/{id}")
    public Topline getTopline(@PathVariable Long id) {
        return toplineService.getById(id);
    }
}
```

- [ ] **Step 8: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=ToplineControllerTest`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/mgwprod/collab/service/ToplineService.java \
        src/main/java/com/mgwprod/collab/controller/ToplineController.java \
        src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java \
        src/test/java/com/mgwprod/collab/controller/ToplineControllerTest.java
git commit -m "feat(collab): add POST/GET /api/toplines with auto-created PENDING collaboration"
```

---

### Task 4: `PUT /api/collaborations/{id}` — aceptar/rechazar (solo el producer dueño del beat)

**Files:**
- Create: `src/main/java/com/mgwprod/collab/service/CollaborationService.java`
- Create: `src/main/java/com/mgwprod/collab/controller/CollaborationController.java`
- Test: `src/test/java/com/mgwprod/collab/service/CollaborationServiceTest.java`
- Test: `src/test/java/com/mgwprod/collab/controller/CollaborationControllerTest.java`

**Interfaces:**
- Consumes: `Collaboration`/`CollaborationStatus`/`CollaborationRepository` (Task 2), `ToplineService.getById(Long)` (Task 3, para llegar del `Collaboration` al `Topline` y de ahí al `Beat`), `com.mgwprod.catalog.repository.BeatRepository`.
- Produces: `CollaborationService.decide(Long collaborationId, Long requestingUserId, CollaborationStatus decision)`.

- [ ] **Step 1: Escribir el test de servicio**

```java
package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private ToplineService toplineService;

    @Mock
    private BeatRepository beatRepository;

    @InjectMocks
    private CollaborationService collaborationService;

    @Test
    void decideAcceptsWhenRequesterOwnsTheBeat() {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        collaboration.setToplineId(10L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collaboration));

        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Beat beat = new Beat();
        beat.setId(2L);
        beat.setProducerId(5L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        when(collaborationRepository.save(collaboration)).thenReturn(collaboration);

        Collaboration result = collaborationService.decide(1L, 5L, CollaborationStatus.ACCEPTED);

        assertThat(result.getStatus()).isEqualTo(CollaborationStatus.ACCEPTED);
        assertThat(result.getDecidedAt()).isNotNull();
    }

    @Test
    void decideThrowsWhenRequesterDoesNotOwnTheBeat() {
        Collaboration collaboration = new Collaboration();
        collaboration.setId(1L);
        collaboration.setToplineId(10L);
        collaboration.setStatus(CollaborationStatus.PENDING);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collaboration));

        Topline topline = new Topline();
        topline.setId(10L);
        topline.setBeatId(2L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Beat beat = new Beat();
        beat.setId(2L);
        beat.setProducerId(5L);
        when(beatRepository.findById(2L)).thenReturn(Optional.of(beat));

        assertThatThrownBy(() -> collaborationService.decide(1L, 999L, CollaborationStatus.ACCEPTED))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=CollaborationServiceTest`
Expected: FAIL

- [ ] **Step 3: Implementar `CollaborationService`**

```java
package com.mgwprod.collab.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.exception.CollaborationNotFoundException;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final ToplineService toplineService;
    private final BeatRepository beatRepository;

    public CollaborationService(CollaborationRepository collaborationRepository,
                                 ToplineService toplineService,
                                 BeatRepository beatRepository) {
        this.collaborationRepository = collaborationRepository;
        this.toplineService = toplineService;
        this.beatRepository = beatRepository;
    }

    @Transactional
    public Collaboration decide(Long collaborationId, Long requestingUserId, CollaborationStatus decision) {
        Collaboration collaboration = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new CollaborationNotFoundException(collaborationId));

        Topline topline = toplineService.getById(collaboration.getToplineId());
        Beat beat = beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new com.mgwprod.catalog.exception.BeatNotFoundException(topline.getBeatId()));

        if (!beat.getProducerId().equals(requestingUserId)) {
            throw new ForbiddenOperationException("Solo el productor dueño del beat puede decidir esta colaboración");
        }

        collaboration.setStatus(decision);
        collaboration.setDecidedAt(Instant.now());
        return collaborationRepository.save(collaboration);
    }

    @Transactional(readOnly = true)
    public List<Collaboration> listByStatus(CollaborationStatus status) {
        if (status != null) {
            return collaborationRepository.findByStatus(status);
        }
        return collaborationRepository.findAll();
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CollaborationServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller**

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollaborationController.class)
class CollaborationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollaborationService collaborationService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void decideReturns200WhenRequesterOwnsTheBeat() throws Exception {
        Collaboration response = new Collaboration();
        response.setId(1L);
        response.setStatus(CollaborationStatus.ACCEPTED);

        when(collaborationService.decide(eq(1L), eq(5L), eq(CollaborationStatus.ACCEPTED)))
                .thenReturn(response);

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void decideReturns403WhenRequesterDoesNotOwnTheBeat() throws Exception {
        when(collaborationService.decide(eq(1L), eq(999L), eq(CollaborationStatus.ACCEPTED)))
                .thenThrow(new ForbiddenOperationException("Solo el productor dueño del beat puede decidir esta colaboración"));

        mockMvc.perform(put("/api/collaborations/1")
                        .requestAttr("userId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void decideReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/collaborations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 6: Correr, verificar que falla**

Run: `./mvnw test -Dtest=CollaborationControllerTest`
Expected: FAIL

- [ ] **Step 7: Implementar `CollaborationController`**

El body de la request es `{"status": "ACCEPTED"}` o `{"status": "REJECTED"}` — se recibe con un record chico `DecisionRequest`, la única excepción a "sin DTOs" porque `Collaboration` completo (con `toplineId`) no debería ser editable por request; solo el campo `status` importa acá. Si preferís mantener cero DTOs en todo el módulo, alternativa: recibir `@RequestParam CollaborationStatus status` en vez de un body — usá esa forma si el grupo prefiere no tener ninguna clase de request.

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collaborations")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PutMapping("/{id}")
    public Collaboration decide(@PathVariable Long id,
                                 @RequestAttribute(name = "userId", required = false) Long userId,
                                 @RequestParam CollaborationStatus status) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para decidir una colaboración");
        }
        return collaborationService.decide(id, userId, status);
    }

    @GetMapping
    public List<Collaboration> list(@RequestParam(required = false) CollaborationStatus status) {
        return collaborationService.listByStatus(status);
    }
}
```

Nota: si elegiste `@RequestParam` en el controller (como el código de arriba), ajustá el test del Step 5 para mandar `status` como query param (`put("/api/collaborations/1").param("status", "ACCEPTED")`) en vez de JSON body — y sacá el `.content(...)`/`.contentType(...)` de esa request.

- [ ] **Step 8: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CollaborationControllerTest`
Expected: PASS

- [ ] **Step 9: Correr toda la suite del módulo y commit**

Run: `./mvnw test -Dtest="com.mgwprod.collab.**"`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/collab/service/CollaborationService.java \
        src/main/java/com/mgwprod/collab/controller/CollaborationController.java \
        src/test/java/com/mgwprod/collab/service/CollaborationServiceTest.java \
        src/test/java/com/mgwprod/collab/controller/CollaborationControllerTest.java
git commit -m "feat(collab): add PUT /api/collaborations/{id} accept/reject flow"
```

---

### Task 5: `Comment` sobre `Topline` + endpoints

**Files:**
- Create: `src/main/java/com/mgwprod/collab/model/Comment.java`
- Create: `src/main/java/com/mgwprod/collab/repository/CommentRepository.java`
- Create: `src/main/java/com/mgwprod/collab/service/CommentService.java`
- Create: `src/main/java/com/mgwprod/collab/controller/CommentController.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/collab/repository/CommentRepositoryTest.java`
- Test: `src/test/java/com/mgwprod/collab/service/CommentServiceTest.java`
- Test: `src/test/java/com/mgwprod/collab/controller/CommentControllerTest.java`

**Interfaces:**
- Produces: `Comment` (id, toplineId, authorId, text, createdAt), `CommentRepository.findByToplineId(Long)`, `CommentService.create(Long toplineId, Long authorId, Comment comment)`, `CommentService.listByTopline(Long toplineId)`.

Este task es idéntico en estructura a `BeatComment` del módulo `catalog` (Task 4-5 de ese plan), reemplazando `beatId` por `toplineId` y validando contra `ToplineService.getById` en vez de `BeatService.getById`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topline_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (topline_id) REFERENCES toplines(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findByToplineIdReturnsOnlyThatToplinesComments() {
        Comment comment = new Comment();
        comment.setToplineId(10L);
        comment.setAuthorId(2L);
        comment.setText("Qué voz");
        commentRepository.save(comment);

        List<Comment> result = commentRepository.findByToplineId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Qué voz");
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=CommentRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear la entidad y el repositorio**

```java
package com.mgwprod.collab.model;

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
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El topline es obligatorio")
    @Column(name = "topline_id", nullable = false)
    private Long toplineId;

    @NotNull(message = "El autor es obligatorio")
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

```java
package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByToplineId(Long toplineId);
}
```

- [ ] **Step 5: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CommentRepositoryTest`
Expected: PASS

- [ ] **Step 6: Escribir el test de servicio**

```java
package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ToplineService toplineService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createSavesCommentWhenToplineExists() {
        Topline topline = new Topline();
        topline.setId(10L);
        when(toplineService.getById(10L)).thenReturn(topline);

        Comment comment = new Comment();
        comment.setText("Qué voz");
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment saved = commentService.create(10L, 2L, comment);

        assertThat(saved.getToplineId()).isEqualTo(10L);
        assertThat(saved.getAuthorId()).isEqualTo(2L);
    }
}
```

- [ ] **Step 7: Correr, verificar que falla**

Run: `./mvnw test -Dtest=CommentServiceTest`
Expected: FAIL

- [ ] **Step 8: Implementar `CommentService`**

```java
package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ToplineService toplineService;

    public CommentService(CommentRepository commentRepository, ToplineService toplineService) {
        this.commentRepository = commentRepository;
        this.toplineService = toplineService;
    }

    @Transactional
    public Comment create(Long toplineId, Long authorId, Comment comment) {
        toplineService.getById(toplineId);
        comment.setToplineId(toplineId);
        comment.setAuthorId(authorId);
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> listByTopline(Long toplineId) {
        toplineService.getById(toplineId);
        return commentRepository.findByToplineId(toplineId);
    }
}
```

- [ ] **Step 9: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CommentServiceTest`
Expected: PASS

- [ ] **Step 10: Escribir el test de controller e implementar `CommentController`**

Test (mismo patrón que `BeatCommentControllerTest` del plan de `catalog`, reemplazando `beatId`→`toplineId` y el servicio):

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.service.CommentService;
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

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createCommentReturns201WhenAuthenticated() throws Exception {
        Comment request = new Comment();
        request.setText("Qué voz");

        Comment response = new Comment();
        response.setId(1L);
        response.setToplineId(10L);
        response.setAuthorId(2L);
        response.setText("Qué voz");

        when(commentService.create(eq(10L), eq(2L), any(Comment.class))).thenReturn(response);

        mockMvc.perform(post("/api/toplines/10/comments")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Qué voz"));
    }

    @Test
    void createCommentReturns401WhenNotAuthenticated() throws Exception {
        Comment request = new Comment();
        request.setText("Qué voz");

        mockMvc.perform(post("/api/toplines/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCommentsReturns200() throws Exception {
        Comment comment = new Comment();
        comment.setText("Qué voz");

        when(commentService.listByTopline(10L)).thenReturn(java.util.List.of(comment));

        mockMvc.perform(get("/api/toplines/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Qué voz"));
    }
}
```

Implementación:

```java
package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.service.CommentService;
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
@RequestMapping("/api/toplines/{toplineId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Comment> createComment(@PathVariable Long toplineId,
                                                  @RequestAttribute(name = "userId", required = false) Long userId,
                                                  @Valid @RequestBody Comment comment) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para comentar");
        }
        Comment created = commentService.create(toplineId, userId, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Comment> listComments(@PathVariable Long toplineId) {
        return commentService.listByTopline(toplineId);
    }
}
```

- [ ] **Step 11: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=CommentControllerTest`
Expected: PASS

- [ ] **Step 12: Correr toda la suite del módulo y commit**

Run: `./mvnw test -Dtest="com.mgwprod.collab.**"`
Expected: PASS

```bash
git add src/main/java/com/mgwprod/collab/model/Comment.java \
        src/main/java/com/mgwprod/collab/repository/CommentRepository.java \
        src/main/java/com/mgwprod/collab/service/CommentService.java \
        src/main/java/com/mgwprod/collab/controller/CommentController.java \
        src/test/java/com/mgwprod/collab/repository/CommentRepositoryTest.java \
        src/test/java/com/mgwprod/collab/service/CommentServiceTest.java \
        src/test/java/com/mgwprod/collab/controller/CommentControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(collab): add comments on toplines"
```

---

## Al terminar

1. Correr `./mvnw test` completo para confirmar que no rompiste nada de `users`/`catalog`.
2. Armar la colección de Postman/`.http` de los 6 endpoints de `collab`.
3. Escribir `docs/api/collab-API.md`.
4. Abrir un PR de `feature/collab-module` contra `main`.
