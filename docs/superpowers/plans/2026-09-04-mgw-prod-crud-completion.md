# CRUD completo (User/Beat/Topline/Collaboration/Challenge) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar los verbos update/delete que faltan en las entidades principales (`User`, `Beat`, `Topline`, `Collaboration`, `Challenge`) para cumplir el pedido del profesor de "CRUD funcional en todas las entidades". Dueño: Paolo.

**Prerequisito:** `feature/roles-migration` (Santiago) ya mergeado a `main` — este plan usa `Role.ADMIN` para los chequeos de autorización, que no existe hasta ese merge.

```bash
git checkout main
git pull
git checkout -b feature/crud-completion
```

**Architecture:** Sin paquetes nuevos. Se toca `users` (`User` delete), `catalog` (`Beat` update/delete), `collab` (`Topline` update/delete, `Collaboration` delete) y `challenges` (`Challenge` update/delete + campo nuevo `createdBy`).

**Tech Stack:** Java 21+, Spring Boot 4.1.0, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-04-mgw-prod-professor-corrections-design.md` (sección 2).

## Global Constraints

- Autorización dueño-o-admin en todos los update/delete: `requester.getRole() == Role.ADMIN || <campo-dueño>.equals(requestingUserId)`.
- **Borrado físico** (no soft-delete), vía FKs con `ON DELETE CASCADE` en `docs/db/schema.sql` — nunca llamadas cruzadas entre módulos para cascadear (`catalog` no debe importar nada de `collab`, ni `challenges` nada de `catalog`, respetando la regla de dependencias del proyecto).
- **Excepción documentada a la regla de dependencias:** el delete de `User` no hace ningún chequeo de "tiene contenido" en código — se apoya en que `beats.producer_id`, `toplines.artist_id`, `challenges.created_by`/`guest_artist_id`, `comments.author_id`, etc. son FKs **sin** cascada hacia `users(id)`. MySQL/InnoDB las deja en `RESTRICT` por default, así que un `DELETE FROM users WHERE id=?` con historial revienta con una violación de integridad referencial — `UserService.delete` la captura y la traduce a 409. Cero imports cruzados desde `users` hacia `catalog`/`collab`/`challenges`.

---

### Task 1: `Beat` — `PUT`/`DELETE /api/beats/{id}`

**Files:**
- Modify: `src/main/java/com/mgwprod/catalog/service/BeatService.java`
- Modify: `src/main/java/com/mgwprod/catalog/controller/BeatController.java`
- Modify: `docs/db/schema.sql`
- Modify: `src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java`
- Modify: `src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java`

- [ ] **Step 1: Cascada en el schema**

En `docs/db/schema.sql`, cambiar las FKs que apuntan a `beats(id)` para que cascadeen:

```sql
CREATE TABLE beat_comments (
    ...
    FOREIGN KEY (beat_id) REFERENCES beats(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id)
);
```

```sql
CREATE TABLE toplines (
    ...
    FOREIGN KEY (artist_id) REFERENCES users(id),
    FOREIGN KEY (beat_id) REFERENCES beats(id) ON DELETE CASCADE
);
```

(Las FKs hacia `users(id)` quedan sin cascada a propósito — ver Global Constraints.)

- [ ] **Step 2: Tests de servicio**

Agregar a `BeatServiceTest`:

```java
    @Test
    void updateAllowsOwnerToChangeFields() {
        Beat existing = new Beat();
        existing.setId(1L);
        existing.setProducerId(1L);
        existing.setTitle("Old Title");
        when(beatRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(beatRepository.save(any(Beat.class))).thenAnswer(inv -> inv.getArgument(0));

        Beat request = new Beat();
        request.setTitle("New Title");

        Beat updated = beatService.update(1L, 1L, request);

        assertThat(updated.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateThrowsWhenRequesterIsNotOwnerOrAdmin() {
        User other = new User();
        other.setId(2L);
        other.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        Beat existing = new Beat();
        existing.setId(1L);
        existing.setProducerId(1L);
        when(beatRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> beatService.update(1L, 2L, new Beat()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteRemovesBeatWhenRequesterIsOwner() {
        Beat existing = new Beat();
        existing.setId(1L);
        existing.setProducerId(1L);
        when(beatRepository.findById(1L)).thenReturn(Optional.of(existing));

        beatService.delete(1L, 1L);

        verify(beatRepository).deleteById(1L);
    }

    @Test
    void deleteAllowsAdminEvenIfNotOwner() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        Beat existing = new Beat();
        existing.setId(1L);
        existing.setProducerId(1L);
        when(beatRepository.findById(1L)).thenReturn(Optional.of(existing));

        beatService.delete(1L, 9L);

        verify(beatRepository).deleteById(1L);
    }
```

Nota: `update`/`delete` solo necesitan `userRepository.findById` cuando el requester **no** es el dueño (para chequear si es admin) — en los tests de "dueño" no hace falta mockear `userRepository`.

- [ ] **Step 2b: Correr, verificar que falla**

Run: `./mvnw test -Dtest=BeatServiceTest`

- [ ] **Step 3: Implementar `update`/`delete` en `BeatService`**

```java
    @Transactional
    public Beat update(Long id, Long requestingUserId, Beat request) {
        Beat beat = getById(id);
        requireOwnerOrAdmin(beat.getProducerId(), requestingUserId);

        if (request.getTitle() != null) beat.setTitle(request.getTitle());
        if (request.getGenre() != null) beat.setGenre(request.getGenre());
        if (request.getBpm() != null) beat.setBpm(request.getBpm());
        if (request.getKey() != null) beat.setKey(request.getKey());
        if (request.getAudioUrl() != null) beat.setAudioUrl(request.getAudioUrl());
        return beatRepository.save(beat);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Beat beat = getById(id);
        requireOwnerOrAdmin(beat.getProducerId(), requestingUserId);
        beatRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(Long ownerId, Long requestingUserId) {
        if (ownerId.equals(requestingUserId)) {
            return;
        }
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo el dueño del beat o un admin pueden hacer esto");
        }
    }
```

- [ ] **Step 4: Correr, verificar que pasa**

Run: `./mvnw test -Dtest=BeatServiceTest`

- [ ] **Step 5: Tests de controller**

Agregar a `BeatControllerTest` (con los imports `put`, `delete` de `MockMvcRequestBuilders`):

```java
    @Test
    void updateBeatReturns200ForOwner() throws Exception {
        Beat response = new Beat();
        response.setId(1L);
        response.setTitle("New Title");
        when(beatService.update(eq(1L), eq(1L), any(Beat.class))).thenReturn(response);

        mockMvc.perform(put("/api/beats/1")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void deleteBeatReturns204ForOwner() throws Exception {
        mockMvc.perform(delete("/api/beats/1").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        verify(beatService).delete(1L, 1L);
    }

    @Test
    void deleteBeatReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/beats/1"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 6: Correr, verificar que falla**

Run: `./mvnw test -Dtest=BeatControllerTest`

- [ ] **Step 7: Endpoints en `BeatController`**

```java
    @PutMapping("/{id}")
    public Beat updateBeat(@PathVariable Long id,
                            @RequestAttribute(name = "userId", required = false) Long userId,
                            @RequestBody Beat beat) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para editar un beat");
        }
        return beatService.update(id, userId, beat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeat(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para borrar un beat");
        }
        beatService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
```

(Sumar los imports `DeleteMapping`, `PutMapping`.)

- [ ] **Step 8: Correr, verificar que pasa; commit**

Run: `./mvnw test -Dtest=BeatControllerTest`

```bash
git add src/main/java/com/mgwprod/catalog/service/BeatService.java \
        src/main/java/com/mgwprod/catalog/controller/BeatController.java \
        src/test/java/com/mgwprod/catalog/service/BeatServiceTest.java \
        src/test/java/com/mgwprod/catalog/controller/BeatControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(catalog): add PUT/DELETE for Beat"
```

---

### Task 2: `Topline` — `PUT`/`DELETE /api/toplines/{id}`

**Files:**
- Modify: `src/main/java/com/mgwprod/collab/service/ToplineService.java`
- Modify: `src/main/java/com/mgwprod/collab/controller/ToplineController.java`
- Modify: `docs/db/schema.sql`
- Modify: `src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java`
- Modify: `src/test/java/com/mgwprod/collab/controller/ToplineControllerTest.java`

Mismo patrón que Task 1, ownership por `Topline.artistId`.

- [ ] **Step 1: Cascada en el schema**

```sql
CREATE TABLE collaborations (
    ...
    FOREIGN KEY (topline_id) REFERENCES toplines(id) ON DELETE CASCADE
);

CREATE TABLE comments (
    ...
    FOREIGN KEY (topline_id) REFERENCES toplines(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id)
);
```

- [ ] **Step 2: Tests de servicio (mismo esquema que Task 1, adaptado a `Topline`/`artistId`)**

Casos: `updateAllowsOwnerToChangeAudioUrl`, `updateThrowsWhenRequesterIsNotOwnerOrAdmin`, `deleteRemovesToplineWhenRequesterIsOwner`, `deleteAllowsAdminEvenIfNotOwner`. Correr, verificar que fallan.

- [ ] **Step 3: `ToplineService.update`/`delete`**

```java
    @Transactional
    public Topline update(Long id, Long requestingUserId, Topline request) {
        Topline topline = getById(id);
        requireOwnerOrAdmin(topline.getArtistId(), requestingUserId);
        if (request.getAudioUrl() != null) {
            topline.setAudioUrl(request.getAudioUrl());
        }
        return toplineRepository.save(topline);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Topline topline = getById(id);
        requireOwnerOrAdmin(topline.getArtistId(), requestingUserId);
        toplineRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(Long ownerId, Long requestingUserId) {
        if (ownerId.equals(requestingUserId)) {
            return;
        }
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo el dueño del topline o un admin pueden hacer esto");
        }
    }
```

(Agregar `import com.mgwprod.users.exception.UserNotFoundException;` si no está.)

- [ ] **Step 4: Correr, verificar que pasa**

- [ ] **Step 5: Tests de controller (mismo esquema que Task 1, adaptado a `/api/toplines/{id}`)**

- [ ] **Step 6: Endpoints en `ToplineController`** (mismo esquema que `BeatController`, con `toplineService.update`/`.delete`)

- [ ] **Step 7: Correr toda la suite de `collab`, confirmar; commit**

```bash
git add src/main/java/com/mgwprod/collab/service/ToplineService.java \
        src/main/java/com/mgwprod/collab/controller/ToplineController.java \
        src/test/java/com/mgwprod/collab/service/ToplineServiceTest.java \
        src/test/java/com/mgwprod/collab/controller/ToplineControllerTest.java \
        docs/db/schema.sql
git commit -m "feat(collab): add PUT/DELETE for Topline"
```

---

### Task 3: `Collaboration` — `DELETE /api/collaborations/{id}`

**Files:**
- Modify: `src/main/java/com/mgwprod/collab/service/CollaborationService.java`
- Modify: `src/main/java/com/mgwprod/collab/controller/CollaborationController.java`
- Modify: `src/test/java/com/mgwprod/collab/service/CollaborationServiceTest.java`
- Modify: `src/test/java/com/mgwprod/collab/controller/CollaborationControllerTest.java`

Autorización: **cualquiera de las dos partes** (el artista dueño del topline, o el productor dueño del beat referenciado) o admin — reutiliza la misma resolución de `Topline`→`Beat` que ya usa `decide`.

- [ ] **Step 1: Test de servicio**

```java
    @Test
    void deleteAllowsTheArtistWhoOwnsTheTopline() {
        Topline topline = new Topline();
        topline.setId(5L);
        topline.setArtistId(2L);
        topline.setBeatId(10L);
        when(toplineService.getById(5L)).thenReturn(topline);

        Collaboration collab = new Collaboration();
        collab.setId(1L);
        collab.setToplineId(5L);
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(collab));

        Beat beat = new Beat();
        beat.setId(10L);
        beat.setProducerId(3L);
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));

        collaborationService.delete(1L, 2L); // artistId

        verify(collaborationRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenRequesterIsNeitherParty() {
        // mismo setup que arriba, pero requestingUserId = 99L (ni artista ni productor)
        // assertThatThrownBy(() -> collaborationService.delete(1L, 99L))
        //         .isInstanceOf(ForbiddenOperationException.class);
    }
```

Correr, verificar que falla.

- [ ] **Step 2: `CollaborationService.delete`**

```java
    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Collaboration collaboration = collaborationRepository.findById(id)
                .orElseThrow(() -> new CollaborationNotFoundException(id));
        Topline topline = toplineService.getById(collaboration.getToplineId());
        Beat beat = beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new BeatNotFoundException(topline.getBeatId()));

        boolean isArtist = topline.getArtistId().equals(requestingUserId);
        boolean isProducer = beat.getProducerId().equals(requestingUserId);
        if (!isArtist && !isProducer) {
            throw new ForbiddenOperationException("Solo las partes de esta colaboración pueden borrarla");
        }
        collaborationRepository.deleteById(id);
    }
```

- [ ] **Step 3: Correr, verificar que pasa**

- [ ] **Step 4: Endpoint `DELETE /api/collaborations/{id}` en `CollaborationController`** (mismo patrón 401/204 que `BeatController`)

- [ ] **Step 5: Test de controller + correr + commit**

```bash
git add src/main/java/com/mgwprod/collab/service/CollaborationService.java \
        src/main/java/com/mgwprod/collab/controller/CollaborationController.java \
        src/test/java/com/mgwprod/collab/service/CollaborationServiceTest.java \
        src/test/java/com/mgwprod/collab/controller/CollaborationControllerTest.java
git commit -m "feat(collab): add DELETE for Collaboration"
```

---

### Task 4: `Challenge` — campo `createdBy` + `PUT`/`DELETE /api/challenges/{id}`

**Files:**
- Modify: `src/main/java/com/mgwprod/challenges/model/Challenge.java`
- Modify: `src/main/java/com/mgwprod/challenges/service/ChallengeService.java`
- Modify: `src/main/java/com/mgwprod/challenges/controller/ChallengeController.java`
- Modify: `src/main/java/com/mgwprod/challenges/repository/ChallengeResultRepository.java`
- Modify: `docs/db/schema.sql`
- Modify: tests de servicio y controller de `Challenge`

`Challenge` hoy no guarda quién lo creó (solo valida que el creador sea admin/discográfica al momento de crearlo) — hace falta el campo para poder chequear ownership en update/delete.

- [ ] **Step 1: Agregar `created_by` al schema**

```sql
CREATE TABLE challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    ...
    FOREIGN KEY (guest_artist_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

Y las cascadas de sus hijos:

```sql
CREATE TABLE submissions (
    ...
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    FOREIGN KEY (producer_id) REFERENCES users(id)
);

CREATE TABLE votes (
    ...
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (voter_id) REFERENCES users(id)
);

CREATE TABLE challenge_results (
    ...
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
);
```

- [ ] **Step 2: `Challenge.java` — sumar `createdBy`**

```java
    // Server-derived desde el requester autenticado (ChallengeService.create) — mismo
    // patrón que Topline.artistId: nunca viaja en el JSON del cliente.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
```

(sumar imports `com.fasterxml.jackson.annotation.JsonProperty`).

- [ ] **Step 3: `ChallengeResultRepository` — sumar `existsByChallengeId`**

```java
    boolean existsByChallengeId(Long challengeId);
```

- [ ] **Step 4: Tests de servicio**

Sumar a `ChallengeServiceTest`:

```java
    @Test
    void createSetsCreatedByToRequester() {
        // requester admin válido (como ya hace el test existente de create) +
        // assertThat(created.getCreatedBy()).isEqualTo(requestingUserId);
    }

    @Test
    void updateAllowsCreatorBeforeClose() {
        // challenge con createdBy=1L, challengeResultRepository.existsByChallengeId(...) = false
        // challengeService.update(challengeId, 1L, request) actualiza title/theme/deadline
    }

    @Test
    void updateThrowsWhenChallengeAlreadyClosed() {
        // challengeResultRepository.existsByChallengeId(...) = true
        // assertThatThrownBy(...).isInstanceOf(ForbiddenOperationException.class)
    }

    @Test
    void deleteAllowsCreatorOrAdmin() {
        // verify(challengeRepository).deleteById(id)
    }
```

Correr, verificar que fallan.

- [ ] **Step 5: `ChallengeService` — setear `createdBy`, sumar `update`/`delete`**

En `create`, después de validar el rol del requester:

```java
        challenge.setCreatedBy(requestingUserId);
        return challengeRepository.save(challenge);
```

Sumar:

```java
    @Transactional
    public Challenge update(Long id, Long requestingUserId, Challenge request) {
        Challenge challenge = getById(id);
        requireOwnerOrAdmin(challenge.getCreatedBy(), requestingUserId);
        if (challengeResultRepository.existsByChallengeId(id)) {
            throw new ForbiddenOperationException("No se puede editar un challenge ya cerrado");
        }
        if (request.getTitle() != null) challenge.setTitle(request.getTitle());
        if (request.getTheme() != null) challenge.setTheme(request.getTheme());
        if (request.getDeadline() != null) challenge.setDeadline(request.getDeadline());
        return challengeRepository.save(challenge);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Challenge challenge = getById(id);
        requireOwnerOrAdmin(challenge.getCreatedBy(), requestingUserId);
        challengeRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(Long ownerId, Long requestingUserId) {
        if (ownerId.equals(requestingUserId)) {
            return;
        }
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo quien creó el challenge o un admin pueden hacer esto");
        }
    }
```

Inyectar `ChallengeResultRepository` en el constructor de `ChallengeService` (cuidado: ya existe una relación `ChallengeResultService → ChallengeService`; inyectar el repositorio, no el service, para no crear un ciclo de beans).

- [ ] **Step 6: Correr, verificar que pasa**

- [ ] **Step 7: Tests + endpoints de controller** (mismo patrón `PUT`/`DELETE` + 401/204 que Task 1)

- [ ] **Step 8: Correr toda la suite de `challenges`, confirmar; commit**

```bash
git add src/main/java/com/mgwprod/challenges \
        src/test/java/com/mgwprod/challenges \
        docs/db/schema.sql
git commit -m "feat(challenges): add createdBy, PUT/DELETE for Challenge"
```

---

### Task 5: `User` — `DELETE /api/users/{id}` (409 si tiene historial)

**Files:**
- Create: `src/main/java/com/mgwprod/users/exception/UserHasContentException.java`
- Modify: `src/main/java/com/mgwprod/users/service/UserService.java`
- Modify: `src/main/java/com/mgwprod/users/controller/UserController.java`
- Modify: `src/test/java/com/mgwprod/users/service/UserServiceTest.java`
- Modify: `src/test/java/com/mgwprod/users/controller/UserControllerTest.java`

- [ ] **Step 1: Excepción nueva**

```java
package com.mgwprod.users.exception;

import com.mgwprod.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserHasContentException extends ApiException {
    public UserHasContentException(Long userId) {
        super(HttpStatus.CONFLICT, "No se puede borrar el usuario " + userId + ": tiene contenido asociado (beats, toplines, challenges, comentarios, votos, etc.)");
    }
}
```

- [ ] **Step 2: Tests de servicio**

```java
    @Test
    void deleteRemovesUserWithNoContent() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L, 1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsConflictWhenUserHasContent() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("FK violation"))
                .when(userRepository).deleteById(1L);
        // deleteById en un mock no ejecuta flush real; se fuerza acá para simular el
        // comportamiento real de MySQL con las FKs sin cascada hacia users.

        assertThatThrownBy(() -> userService.delete(1L, 1L))
                .isInstanceOf(UserHasContentException.class);
    }

    @Test
    void deleteThrowsWhenRequesterIsNotOwnerOrAdmin() {
        User target = new User();
        target.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        User requester = new User();
        requester.setId(2L);
        requester.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> userService.delete(1L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
```

Correr, verificar que fallan.

- [ ] **Step 3: `UserService.delete`**

```java
    @Transactional
    public void delete(Long targetUserId, Long requestingUserId) {
        User target = getById(targetUserId);
        if (!targetUserId.equals(requestingUserId)) {
            User requester = userRepository.findById(requestingUserId)
                    .orElseThrow(() -> new UserNotFoundException(requestingUserId));
            if (requester.getRole() != Role.ADMIN) {
                throw new ForbiddenOperationException("Solo el propio usuario o un admin pueden borrar esta cuenta");
            }
        }
        try {
            userRepository.deleteById(targetUserId);
            userRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new UserHasContentException(targetUserId);
        }
    }
```

`flush()` es necesario porque `deleteById` dentro de una transacción puede diferir el `DELETE` real de MySQL hasta el commit — sin el flush explícito, la violación de FK no se detectaría acá sino al cerrar la transacción, demasiado tarde para convertirla en una excepción de negocio limpia.

- [ ] **Step 4: Correr, verificar que pasa**

- [ ] **Step 5: Endpoint en `UserController`**

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        requireAuthenticated(requestingUserId);
        userService.delete(id, requestingUserId);
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 6: Test de controller (200→204, 401, 409 mockeando `UserHasContentException`) + correr + commit**

```bash
git add src/main/java/com/mgwprod/users/exception/UserHasContentException.java \
        src/main/java/com/mgwprod/users/service/UserService.java \
        src/main/java/com/mgwprod/users/controller/UserController.java \
        src/test/java/com/mgwprod/users/service/UserServiceTest.java \
        src/test/java/com/mgwprod/users/controller/UserControllerTest.java
git commit -m "feat(users): add DELETE for User, 409 when the user has content"
```

---

### Task 6: Suite completa, docs, recreación de la base local

- [ ] **Step 1: Correr toda la suite**

Run: `./mvnw test`
Expected: PASS

- [ ] **Step 2: Recrear la base local** (schema.sql cambió: nuevas cascadas + `challenges.created_by`)

```bash
mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

- [ ] **Step 3: Smoke test manual mínimo**

Con la app levantada: crear un beat, editarlo (`PUT`), borrarlo (`DELETE`, confirmar 204 y que `GET` da 404 después); repetir para topline; borrar una collaboration; crear un challenge, editarlo, borrarlo; intentar borrar un usuario con beats (esperar 409) y uno sin nada (esperar 204).

- [ ] **Step 4: Actualizar `docs/api/*.md`**

Sumar las secciones de los nuevos endpoints a `catalog-API.md`, `collab-API.md`, `challenges-API.md`, `users-API.md` (mismo formato que las existentes).

- [ ] **Step 5: Commit final y push**

```bash
git add docs/api
git commit -m "docs: document the new CRUD endpoints"
git push -u origin feature/crud-completion
```

## Al terminar

Abrir PR de `feature/crud-completion` contra `main`.
