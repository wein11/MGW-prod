package com.mgwprod.users.controller;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints REST para leer/editar un usuario y su perfil de artista. Devuelve las
// entidades JPA directo (sin DTOs) y decide cada código de estado con `if` +
// ResponseEntity a mano — acá no se lanza ninguna excepción.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Público — cualquiera (incluso sin sesión) puede buscar el perfil público de un
    // usuario por id.
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<ArtistProfile> getProfile(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        // 403 y no 404: el usuario existe, simplemente no tiene perfil de artista
        // porque no es artista.
        if (!userService.isArtist(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(userService.getProfile(id));
    }

    // Acá importa el orden de los chequeos: auth (401) -> ownership (403) -> existe (404).
    // Se chequea ownership antes que existencia para que alguien probando un id que no
    // existe reciba el mismo 403 que si probara uno real que no le pertenece — el
    // chequeo de 401 va primero solo porque "no estar logueado" siempre gana.
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                            @RequestBody User request) {
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (!userService.isOwner(id, requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        User existing = userService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PutMapping("/{id}/artist-profile")
    public ResponseEntity<ArtistProfile> updateArtistProfile(@PathVariable Long id,
                                                              @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                                              @RequestBody ArtistProfile request) {
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (!userService.isOwner(id, requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        User existing = userService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!userService.isArtist(existing)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(userService.updateArtistProfile(id, request));
    }

    // Un usuario puede borrar su propia cuenta (isOwner), O un admin puede borrar
    // cualquiera — por eso acá un ownership check fallido no devuelve 403 de una como
    // en updateUser: primero se pasa a chequear el caso de admin.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User target = userService.getById(id);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!userService.isOwner(id, requestingUserId)) {
            User requester = userService.getById(requestingUserId);
            if (requester == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (!userService.isAdmin(requester)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        // 409 CONFLICT: el usuario existe y el que llama tiene permiso para borrarlo,
        // pero la base no lo permite porque todavía tiene otras filas asociadas (beats,
        // submissions, etc.) — ver el manejo de DataIntegrityViolationException en
        // UserService.delete.
        if (!userService.delete(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.noContent().build();
    }
}
