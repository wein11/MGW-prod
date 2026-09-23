package com.mgwprod.users.repository;

import com.mgwprod.users.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
}
