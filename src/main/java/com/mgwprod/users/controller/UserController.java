package com.mgwprod.users.controller;

import com.mgwprod.users.dto.UpdateUserRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.service.UserService;
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
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                    @RequestAttribute(name = "userId", required = false) Long requestingUserId,
                                    @RequestBody UpdateUserRequest request) {
        if (requestingUserId == null) {
            throw new ForbiddenOperationException("Necesitás iniciar sesión para editar un perfil");
        }
        return userService.update(id, requestingUserId, request);
    }
}
