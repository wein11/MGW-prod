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

@RestController
public class ArtistVerificationController {

    private final UserService userService;

    public ArtistVerificationController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/api/artists/{id}/verify")
    public ResponseEntity<ArtistProfile> verify(@PathVariable Long id,
                                                 @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        User requester = userService.getById(requestingUserId);
        if (requester == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!userService.isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        User artist = userService.getById(id);
        if (artist == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!userService.isArtist(artist)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(userService.verifyArtist(id));
    }
}
