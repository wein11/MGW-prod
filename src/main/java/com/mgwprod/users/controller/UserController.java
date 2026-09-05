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
