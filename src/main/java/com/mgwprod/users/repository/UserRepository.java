package com.mgwprod.users.repository;

import com.mgwprod.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository de Spring Data JPA: no hace falta escribir ninguna implementación. Spring
// lee el nombre del método y arma el SQL solo, en base a esa convención de nombres.
public interface UserRepository extends JpaRepository<User, Long> {
    // findByEmail -> SELECT * FROM users WHERE email = ?  (se usa para buscar al usuario en el login)
    Optional<User> findByEmail(String email);
    // existsByEmail -> SELECT COUNT(*) > 0 ... (más barato que findByEmail cuando solo
    // hace falta un sí/no, por ejemplo para rechazar un registro duplicado con 409)
    boolean existsByEmail(String email);
}
