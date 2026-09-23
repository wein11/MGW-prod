package com.mgwprod.users.repository;

import com.mgwprod.users.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    // La consulta sobre la que corre todo el flujo de auth: SessionAuthInterceptor la
    // llama en cada request autenticado para convertir el token Bearer de vuelta en un User.
    Optional<Session> findByToken(String token);
}
