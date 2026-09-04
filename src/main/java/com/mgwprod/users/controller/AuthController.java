package com.mgwprod.users.controller;

import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User created = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // No @Valid: reusa la entidad User como carrier de credenciales (mismo patrón
    // "sin DTOs" que register), pero un login no trae displayName/role, así que
    // no puede pasar la validación completa de User — solo se leen email/password.
    @PostMapping("/login")
    public ResponseEntity<Session> login(@RequestBody User credentials) {
        return ResponseEntity.ok(authService.login(credentials.getEmail(), credentials.getPassword()));
    }
}
