package com.elyella.backend.repository;

import com.elyella.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Busca un usuario por su correo electrónico. */
    Optional<User> findByEmail(String email);

    /** Verifica si ya existe un usuario con el correo dado. */
    boolean existsByEmail(String email);
}
