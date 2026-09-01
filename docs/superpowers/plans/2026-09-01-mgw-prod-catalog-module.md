# Módulo `catalog` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el módulo `catalog` (publicar/listar beats + comentarios) del pivot de mgw-prod, dueño: Paolo.

**Architecture:** Paquete vertical `com.mgwprod.catalog` (controller/service/repository/model/exception), calcado del patrón ya usado en `com.mgwprod.users`: sin DTOs (controllers reciben/devuelven la entidad JPA directo), Service con la lógica de negocio, Repository `JpaRepository` sin lógica, `@RestControllerAdvice` global ya existente (`com.mgwprod.common.exception.GlobalExceptionHandler`) maneja los errores. `catalog` solo referencia `User` (de `users`) por FK plana (`Long producerId`/`authorId`), nunca al revés.

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` (secciones "Módulo `catalog`" y "Endpoints").

## Global Constraints

- **Sin DTOs**: los controllers reciben y devuelven las entidades JPA (`Beat`, `BeatComment`) directamente, nunca un DTO propio.
- **Validación solo en el controller**: `spring.jpa.properties.jakarta.persistence.validation.mode=none` está seteado a nivel app (viene de la rama `refactor/users-remove-dtos`, mergeada antes de arrancar este plan — ver prerequisito abajo). Usar siempre `@Valid @RequestBody` en el controller; nunca asumir que Hibernate valida al hacer `save()`.
- **Esquema manual**: `spring.jpa.hibernate.ddl-auto=none`. Cualquier tabla nueva se suma a mano a `docs/db/schema.sql` (al final del archivo) y hay que correr ese script contra MySQL local antes de levantar la app.
- **Auth casera**: `SessionAuthInterceptor` (`com.mgwprod.users.security`) ya intercepta todo `/api/**` salvo `/api/auth/**`. Si el request trae un Bearer token válido, setea los request attributes `userId` (Long) y `userRole` (String: `"PRODUCER"`/`"ARTIST"`); si no trae token, dejar pasar el request sin esos attributes (no rechaza automáticamente — cada controller decide si el endpoint requiere autenticación).
- **Nunca commitear a `main` directo** — todo el trabajo de este plan va en una branch nueva (ver Prerequisito).
- **FK cruzada solo hacia `users`, nunca al revés**: usar `com.mgwprod.users.repository.UserRepository` y `com.mgwprod.users.model.{User,Role}` para validar que un `producerId`/`authorId` existe y tiene el rol correcto. No importar nada de `catalog` desde `users`.

## Prerequisito (una sola vez, antes de la Task 1)

El PR de "sin DTOs" (`refactor/users-remove-dtos`) tiene que estar mergeado a `main` antes de arrancar — este plan asume que `UserController`/`UserService` ya devuelven entidades directo y que `spring.jpa.properties.jakarta.persistence.validation.mode=none` ya está en `application.properties`. Confirmá con Santiago que el merge ya pasó. Después:

```bash
git checkout main
git pull
git checkout -b feature/catalog-module
```

---

### Task 1: Entidad `Beat` + repositorio + schema

**Files:**
- Create: `src/main/java/com/mgwprod/catalog/model/Beat.java`
- Create: `src/main/java/com/mgwprod/catalog/repository/BeatRepository.java`
- Create: `src/main/java/com/mgwprod/catalog/exception/BeatNotFoundException.java`
- Modify: `docs/db/schema.sql` (agregar al final)
- Test: `src/test/java/com/mgwprod/catalog/repository/BeatRepositoryTest.java`

**Interfaces:**
- Produces: `Beat` (id, producerId, title, genre, bpm, key, audioUrl, createdAt — todos getters/setters vía Lombok), `BeatRepository.findByProducerId(Long)`, `.findByGenre(String)`, `.findByBpm(Integer)`, `.findByGenreAndBpm(String, Integer)`, `BeatNotFoundException(Long beatId)`.

- [ ] **Step 1: Agregar la tabla al schema**

Sumar al final de `docs/db/schema.sql`:

```sql
CREATE TABLE beats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producer_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    bpm INT NOT NULL,
    music_key VARCHAR(20),
    audio_url VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (producer_id) REFERENCES users(id)
);
```

Correr contra tu MySQL local:
```bash
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```
(Si la base ya tenía las tablas de `users`, MySQL va a fallar con "table already exists" en esas — es esperable; lo importante es que la tabla `beats` quede creada. Si preferís, copiá solo el bloque `CREATE TABLE beats` a mano.)

- [ ] **Step 2: Escribir el test de repositorio (falla al no existir la clase)**

```java
package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.Beat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class BeatRepositoryTest {

    @Autowired
    private BeatRepository beatRepository;

    @Test
    void findByProducerIdReturnsOnlyThatProducersBeats() {
        Beat beat = new Beat();
        beat.setProducerId(1L);
        beat.setTitle("Trap Beat");
        beat.setGenre("Trap");
        beat.setBpm(140);
        beat.setAudioUrl("https://soundcloud.com/example/trap-beat");
        beatRepository.save(beat);

        List<Beat> result = beatRepository.findByProducerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Trap Beat");
    }
}
```

Nota: `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = ANY)` usa una base H2 en memoria en vez de tu MySQL local — no necesita el schema manual para este test puntual (Hibernate genera el esquema de test automáticamente a partir de la entidad). Si el proyecto no tiene la dependencia `com.h2database:h2` en `pom.xml`, agregala con scope `test` antes de este paso.

- [ ] **Step 3: Correr el test, verificar que falla**

Run: `./mvnw test -Dtest=BeatRepositoryTest`
Expected: FAIL (no compila — `Beat` y `BeatRepository` no existen todavía).

- [ ] **Step 4: Crear la entidad `Beat`**

```java
package com.mgwprod.catalog.model;

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
@Table(name = "beats")
@Getter
@Setter
@NoArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producer es obligatorio")
    @Column(name = "producer_id", nullable = false)
    private Long producerId;

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
package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.Beat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatRepository extends JpaRepository<Beat, Long> {
    List<Beat> findByProducerId(Long producerId);
    List<Beat> findByGenre(String genre);
    List<Beat> findByBpm(Integer bpm);
    List<Beat> findByGenreAndBpm(String genre, Integer bpm);
}
```

- [ ] **Step 6: Crear la excepción**

```java
package com.mgwprod.catalog.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BeatNotFoundException extends ApiException {
    public BeatNotFoundException(Long beatId) {
        super(HttpStatus.NOT_FOUND, "No existe un beat con id: " + beatId);
    }
}
```

- [ ] **Step 7: Correr el test, verificar que pasa**

Run: `./mvnw test -Dtest=BeatRepositoryTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mgwprod/catalog/model/Beat.java \
        src/main/java/com/mgwprod/catalog/repository/BeatRepository.java \
        src/main/java/com/mgwprod/catalog/exception/BeatNotFoundException.java \
        src/test/java/com/mgwprod/catalog/repository/BeatRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(catalog): add Beat entity, repository, and schema"
```

---

### Task 2: `POST /api/beats` — publicar beat (rol PRODUCER)

**Files:**
- Create: `src/main/java/com/mgwprod/catalog/service/BeatService.java`
- Create: `src/main/java/com/mgwprod/catalog/controller/BeatController.java`
- Test: `src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java`
- Test: `src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java`

**Interfaces:**
- Consumes: `Beat` (Task 1), `BeatRepository` (Task 1), `com.mgwprod.users.repository.UserRepository` (ya existe en `users`), `com.mgwprod.users.model.{User, Role}` (ya existen), `com.mgwprod.users.exception.{UnauthenticatedException, ForbiddenOperationException}` (ya existen).
- Produces: `BeatService.create(Long producerId, Beat beat)`, `BeatController` con `POST /api/beats`.

- [ ] **Step 1: Escribir el test de servicio (falla al no existir la clase)**

```java
package com.mgwprod.catalog.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeatServiceTest {

    @Mock
    private BeatRepository beatRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BeatService beatService;

    @Test
    void createSavesBeatWhenProducerIsValid() {
        User producer = new User();
        producer.setId(1L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(producer));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");
        beat.setGenre("Trap");
        beat.setBpm(140);
        beat.setAudioUrl("https://soundcloud.com/example/trap-beat");

        when(beatRepository.save(any(Beat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Beat saved = beatService.create(1L, beat);

        assertThat(saved.getProducerId()).isEqualTo(1L);
    }

    @Test
    void createThrowsWhenUserIsNotProducer() {
        User artist = new User();
        artist.setId(2L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));

        Beat beat = new Beat();
        beat.setTitle("Trap Beat");

        assertThatThrownBy(() -> beatService.create(2L, beat))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
```

- [ ] **Step 2: Correr el test, verificar que falla**

Run: `./mvnw test -Dtest=BeatServiceTest`
Expected: FAIL (no compila — `BeatService` no existe).

- [ ] **Step 3: Implementar `BeatService.create`**

```java
package com.mgwprod.catalog.service;

import com.mgwprod.catalog.exception.BeatNotFoundException;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeatService {

    private final BeatRepository beatRepository;
    private final UserRepository userRepository;

    public BeatService(BeatRepository beatRepository, UserRepository userRepository) {
        this.beatRepository = beatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Beat create(Long producerId, Beat beat) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Solo un productor puede publicar beats");
        }
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }

    @Transactional(readOnly = true)
    public List<Beat> list(String genre, Integer bpm, Long producerId) {
        if (producerId != null) {
            return beatRepository.findByProducerId(producerId);
        }
        if (genre != null && bpm != null) {
            return beatRepository.findByGenreAndBpm(genre, bpm);
        }
        if (genre != null) {
            return beatRepository.findByGenre(genre);
        }
        if (bpm != null) {
            return beatRepository.findByBpm(bpm);
        }
        return beatRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Beat getById(Long id) {
        return beatRepository.findById(id)
                .orElseThrow(() -> new BeatNotFoundException(id));
    }
}
```

- [ ] **Step 4: Correr el test, verificar que pasa**

Run: `./mvnw test -Dtest=BeatServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller**

```java
package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.service.BeatService;
import com.mgwprod.users.exception.ForbiddenOperationException;
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

@WebMvcTest(BeatController.class)
class BeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BeatService beatService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createBeatReturns201WhenAuthenticatedAsProducer() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        Beat response = new Beat();
        response.setId(1L);
        response.setProducerId(1L);
        response.setTitle("Trap Beat");

        when(beatService.create(eq(1L), any(Beat.class))).thenReturn(response);

        mockMvc.perform(post("/api/beats")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "PRODUCER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Trap Beat"));
    }

    @Test
    void createBeatReturns401WhenNotAuthenticated() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        mockMvc.perform(post("/api/beats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBeatReturns403WhenAuthenticatedAsArtist() throws Exception {
        Beat request = new Beat();
        request.setTitle("Trap Beat");
        request.setGenre("Trap");
        request.setBpm(140);
        request.setAudioUrl("https://soundcloud.com/example/trap-beat");

        when(beatService.create(eq(2L), any(Beat.class)))
                .thenThrow(new ForbiddenOperationException("Solo un productor puede publicar beats"));

        mockMvc.perform(post("/api/beats")
                        .requestAttr("userId", 2L)
                        .requestAttr("userRole", "ARTIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 6: Correr el test, verificar que falla**

Run: `./mvnw test -Dtest=BeatControllerTest`
Expected: FAIL (no compila — `BeatController` no existe).

- [ ] **Step 7: Implementar `BeatController` (solo el endpoint de creación por ahora)**

```java
package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.service.BeatService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beats")
public class BeatController {

    private final BeatService beatService;

    public BeatController(BeatService beatService) {
        this.beatService = beatService;
    }

    @PostMapping
    public ResponseEntity<Beat> createBeat(@RequestAttribute(name = "userId", required = false) Long userId,
                                            @Valid @RequestBody Beat beat) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para publicar un beat");
        }
        Beat created = beatService.create(userId, beat);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

- [ ] **Step 8: Correr el test, verificar que pasa**

Run: `./mvnw test -Dtest=BeatControllerTest`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/mgwprod/catalog/service/BeatService.java \
        src/main/java/com/mgwprod/catalog/controller/BeatController.java \
        src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java \
        src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java
git commit -m "feat(catalog): add POST /api/beats"
```

---

### Task 3: `GET /api/beats` y `GET /api/beats/{id}`

**Files:**
- Modify: `src/main/java/com/mgwprod/catalog/controller/BeatController.java`
- Modify: `src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java`

**Interfaces:**
- Consumes: `BeatService.list(String, Integer, Long)` y `BeatService.getById(Long)` (ya existen desde Task 2).

- [ ] **Step 1: Agregar los tests de listado y detalle**

Sumar a `BeatControllerTest`:

```java
    @Test
    void listBeatsReturns200WithFilteredResults() throws Exception {
        Beat beat = new Beat();
        beat.setId(1L);
        beat.setGenre("Trap");
        beat.setBpm(140);

        when(beatService.list("Trap", null, null)).thenReturn(java.util.List.of(beat));

        mockMvc.perform(get("/api/beats").param("genre", "Trap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre").value("Trap"));
    }

    @Test
    void getBeatByIdReturns200WhenExists() throws Exception {
        Beat beat = new Beat();
        beat.setId(1L);
        beat.setTitle("Trap Beat");

        when(beatService.getById(1L)).thenReturn(beat);

        mockMvc.perform(get("/api/beats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Trap Beat"));
    }

    @Test
    void getBeatByIdReturns404WhenMissing() throws Exception {
        when(beatService.getById(99L))
                .thenThrow(new com.mgwprod.catalog.exception.BeatNotFoundException(99L));

        mockMvc.perform(get("/api/beats/99"))
                .andExpect(status().isNotFound());
    }
```

Agregar el import estático `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;` al inicio del archivo.

- [ ] **Step 2: Correr los tests, verificar que fallan**

Run: `./mvnw test -Dtest=BeatControllerTest`
Expected: FAIL (los endpoints GET no existen todavía).

- [ ] **Step 3: Agregar los endpoints al controller**

Sumar a `BeatController` (con los imports `GetMapping`, `PathVariable`, `RequestParam`):

```java
    @GetMapping
    public java.util.List<Beat> listBeats(@RequestParam(required = false) String genre,
                                           @RequestParam(required = false) Integer bpm,
                                           @RequestParam(required = false) Long producerId) {
        return beatService.list(genre, bpm, producerId);
    }

    @GetMapping("/{id}")
    public Beat getBeat(@PathVariable Long id) {
        return beatService.getById(id);
    }
```

- [ ] **Step 4: Correr los tests, verificar que pasan**

Run: `./mvnw test -Dtest=BeatControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mgwprod/catalog/controller/BeatController.java \
        src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java
git commit -m "feat(catalog): add GET /api/beats and GET /api/beats/{id}"
```

---

### Task 4: `BeatComment` — entidad, repositorio y schema

**Files:**
- Create: `src/main/java/com/mgwprod/catalog/model/BeatComment.java`
- Create: `src/main/java/com/mgwprod/catalog/repository/BeatCommentRepository.java`
- Modify: `docs/db/schema.sql`
- Test: `src/test/java/com/mgwprod/catalog/repository/BeatCommentRepositoryTest.java`

**Interfaces:**
- Produces: `BeatComment` (id, beatId, authorId, text, createdAt), `BeatCommentRepository.findByBeatId(Long)`.

- [ ] **Step 1: Agregar la tabla al schema**

```sql
CREATE TABLE beat_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    beat_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (beat_id) REFERENCES beats(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Escribir el test de repositorio**

```java
package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.BeatComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class BeatCommentRepositoryTest {

    @Autowired
    private BeatCommentRepository beatCommentRepository;

    @Test
    void findByBeatIdReturnsOnlyThatBeatsComments() {
        BeatComment comment = new BeatComment();
        comment.setBeatId(1L);
        comment.setAuthorId(2L);
        comment.setText("Está buenísimo el beat");
        beatCommentRepository.save(comment);

        List<BeatComment> result = beatCommentRepository.findByBeatId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Está buenísimo el beat");
    }
}
```

- [ ] **Step 3: Correr, verificar que falla**

Run: `./mvnw test -Dtest=BeatCommentRepositoryTest`
Expected: FAIL

- [ ] **Step 4: Crear la entidad**

```java
package com.mgwprod.catalog.model;

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
@Table(name = "beat_comments")
@Getter
@Setter
@NoArgsConstructor
public class BeatComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El beat es obligatorio")
    @Column(name = "beat_id", nullable = false)
    private Long beatId;

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

- [ ] **Step 5: Crear el repositorio**

```java
package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.BeatComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatCommentRepository extends JpaRepository<BeatComment, Long> {
    List<BeatComment> findByBeatId(Long beatId);
}
```

- [ ] **Step 6: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=BeatCommentRepositoryTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/mgwprod/catalog/model/BeatComment.java \
        src/main/java/com/mgwprod/catalog/repository/BeatCommentRepository.java \
        src/test/java/com/mgwprod/catalog/repository/BeatCommentRepositoryTest.java \
        docs/db/schema.sql
git commit -m "feat(catalog): add BeatComment entity, repository, and schema"
```

---

### Task 5: `POST /api/beats/{id}/comments` y `GET /api/beats/{id}/comments`

**Files:**
- Create: `src/main/java/com/mgwprod/catalog/service/BeatCommentService.java`
- Create: `src/main/java/com/mgwprod/catalog/controller/BeatCommentController.java`
- Test: `src/test/java/com/mgwprod/catalog/service/BeatCommentServiceTest.java`
- Test: `src/test/java/com/mgwprod/catalog/controller/BeatCommentControllerTest.java`

**Interfaces:**
- Consumes: `BeatComment`, `BeatCommentRepository` (Task 4), `BeatService.getById(Long)` (Task 2, para validar que el beat existe), `UnauthenticatedException` (de `users.exception`).
- Produces: `BeatCommentService.create(Long beatId, Long authorId, BeatComment comment)`, `BeatCommentService.listByBeat(Long beatId)`.

- [ ] **Step 1: Escribir el test de servicio**

```java
package com.mgwprod.catalog.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.repository.BeatCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeatCommentServiceTest {

    @Mock
    private BeatCommentRepository beatCommentRepository;

    @Mock
    private BeatService beatService;

    @InjectMocks
    private BeatCommentService beatCommentService;

    @Test
    void createSavesCommentWhenBeatExists() {
        Beat beat = new Beat();
        beat.setId(1L);
        when(beatService.getById(1L)).thenReturn(beat);

        BeatComment comment = new BeatComment();
        comment.setText("Buenísimo");
        when(beatCommentRepository.save(any(BeatComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BeatComment saved = beatCommentService.create(1L, 2L, comment);

        assertThat(saved.getBeatId()).isEqualTo(1L);
        assertThat(saved.getAuthorId()).isEqualTo(2L);
    }
}
```

- [ ] **Step 2: Correr, verificar que falla**

Run: `./mvnw test -Dtest=BeatCommentServiceTest`
Expected: FAIL

- [ ] **Step 3: Implementar `BeatCommentService`**

```java
package com.mgwprod.catalog.service;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.repository.BeatCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeatCommentService {

    private final BeatCommentRepository beatCommentRepository;
    private final BeatService beatService;

    public BeatCommentService(BeatCommentRepository beatCommentRepository, BeatService beatService) {
        this.beatCommentRepository = beatCommentRepository;
        this.beatService = beatService;
    }

    @Transactional
    public BeatComment create(Long beatId, Long authorId, BeatComment comment) {
        beatService.getById(beatId); // valida que el beat exista, lanza BeatNotFoundException si no
        comment.setBeatId(beatId);
        comment.setAuthorId(authorId);
        return beatCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<BeatComment> listByBeat(Long beatId) {
        beatService.getById(beatId);
        return beatCommentRepository.findByBeatId(beatId);
    }
}
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=BeatCommentServiceTest`
Expected: PASS

- [ ] **Step 5: Escribir el test de controller**

```java
package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.service.BeatCommentService;
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

@WebMvcTest(BeatCommentController.class)
class BeatCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BeatCommentService beatCommentService;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Test
    void createCommentReturns201WhenAuthenticated() throws Exception {
        BeatComment request = new BeatComment();
        request.setText("Buenísimo");

        BeatComment response = new BeatComment();
        response.setId(1L);
        response.setBeatId(1L);
        response.setAuthorId(2L);
        response.setText("Buenísimo");

        when(beatCommentService.create(eq(1L), eq(2L), any(BeatComment.class))).thenReturn(response);

        mockMvc.perform(post("/api/beats/1/comments")
                        .requestAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Buenísimo"));
    }

    @Test
    void createCommentReturns401WhenNotAuthenticated() throws Exception {
        BeatComment request = new BeatComment();
        request.setText("Buenísimo");

        mockMvc.perform(post("/api/beats/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCommentsReturns200() throws Exception {
        BeatComment comment = new BeatComment();
        comment.setText("Buenísimo");

        when(beatCommentService.listByBeat(1L)).thenReturn(java.util.List.of(comment));

        mockMvc.perform(get("/api/beats/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Buenísimo"));
    }
}
```

- [ ] **Step 6: Correr, verificar que falla**

Run: `./mvnw test -Dtest=BeatCommentControllerTest`
Expected: FAIL

- [ ] **Step 7: Implementar `BeatCommentController`**

```java
package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.service.BeatCommentService;
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
@RequestMapping("/api/beats/{beatId}/comments")
public class BeatCommentController {

    private final BeatCommentService beatCommentService;

    public BeatCommentController(BeatCommentService beatCommentService) {
        this.beatCommentService = beatCommentService;
    }

    @PostMapping
    public ResponseEntity<BeatComment> createComment(@PathVariable Long beatId,
                                                       @RequestAttribute(name = "userId", required = false) Long userId,
                                                       @Valid @RequestBody BeatComment comment) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para comentar");
        }
        BeatComment created = beatCommentService.create(beatId, userId, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<BeatComment> listComments(@PathVariable Long beatId) {
        return beatCommentService.listByBeat(beatId);
    }
}
```

- [ ] **Step 8: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=BeatCommentControllerTest`
Expected: PASS

- [ ] **Step 9: Correr toda la suite del módulo y commit**

Run: `./mvnw test -Dtest="com.mgwprod.catalog.**"`
Expected: PASS (todos los tests de `catalog`)

```bash
git add src/main/java/com/mgwprod/catalog/service/BeatCommentService.java \
        src/main/java/com/mgwprod/catalog/controller/BeatCommentController.java \
        src/test/java/com/mgwprod/catalog/service/BeatCommentServiceTest.java \
        src/test/java/com/mgwprod/catalog/controller/BeatCommentControllerTest.java
git commit -m "feat(catalog): add beat comments endpoints"
```

---

## Al terminar

1. Correr `./mvnw test` completo una vez más para confirmar que no rompiste nada de `users`.
2. Armar una colección de Postman (o `.http`) con los 5 endpoints de `catalog` — sirve de prueba de Etapa 1 y de guión para el oral.
3. Escribir `docs/api/catalog-API.md` (mismo formato que `docs/api/users-API.md`) documentando los endpoints.
4. Abrir un PR de `feature/catalog-module` contra `main` — nunca mergear directo.
