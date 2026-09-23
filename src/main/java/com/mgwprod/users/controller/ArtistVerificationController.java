package com.mgwprod.users.controller;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

// Es su propio controller chico en vez de un método más de UserController: verificar
// un artista es una acción de moderación/admin, conceptualmente distinta a que un
// usuario gestione su propia cuenta, por eso tiene su propia URL bajo /api/artists en
// vez de /api/users.
@RestController
public class ArtistVerificationController {

    private final UserService userService;

    public ArtistVerificationController(UserService userService) {
        this.userService = userService;
    }

    // PUT /api/artists/{id}/verify -> un admin marca a un artista como verificado.
    @PutMapping("/api/artists/{id}/verify")
    public ResponseEntity<ArtistProfile> verify(@PathVariable Long id,
                                                 @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        // 1) tiene que estar logueado
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        // 2) el usuario de la sesión tiene que seguir existiendo
        User requester = userService.getById(requestingUserId);
        if (requester == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        // 3) y específicamente ser admin — solo un admin puede verificar artistas
        if (!userService.isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        // 4) el id destino tiene que existir...
        User artist = userService.getById(id);
        if (artist == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        // 5) ...y ser realmente un artista (verificar una cuenta de sello/admin no tiene sentido)
        if (!userService.isArtist(artist)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(userService.verifyArtist(id));
    }
}
