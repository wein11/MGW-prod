package com.mgwprod.users.controller;

import com.mgwprod.users.model.Session;
import com.mgwprod.users.model.User;
import com.mgwprod.users.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Los dos únicos endpoints excluidos de SessionAuthInterceptor (ver WebConfig) — no
// hace falta tener sesión para crear una o para loguearse.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Cada chequeo de 400 acá es un `if` manual en vez de @Valid/@NotBlank en la entidad.
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body(null);
        }
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (user.getRole() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        if (authService.emailExists(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        User created = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Acá no hace falta ningún chequeo manual de campos: si algo viene vacío o mal,
    // simplemente no va a coincidir con ningún usuario real dentro de AuthService.login,
    // que ya junta todos los casos de fallo (campos faltantes incluidos) en el mismo
    // null -> 401.
    @PostMapping("/login")
    public ResponseEntity<Session> login(@RequestBody User credentials) {
        Session session = authService.login(credentials.getEmail(), credentials.getPassword());
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(session);
    }
}
