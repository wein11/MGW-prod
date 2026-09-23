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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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
        if (!userService.isArtist(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(userService.getProfile(id));
    }

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

    // Puede borrar el propio usuario o un admin. 409 si todavía tiene beats/entregas asociadas.
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
        if (!userService.delete(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.noContent().build();
    }
}
