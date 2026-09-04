package com.mgwprod.users.controller;

import com.mgwprod.users.exception.UnauthenticatedException;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.service.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerVerificationController {

    private final UserService userService;

    public ProducerVerificationController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/api/producers/{id}/verify")
    public ProducerProfile verify(@PathVariable Long id,
                                   @RequestAttribute(name = "userId", required = false) Long requestingUserId) {
        if (requestingUserId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para verificar un productor");
        }
        return userService.verifyProducer(requestingUserId, id);
    }
}
