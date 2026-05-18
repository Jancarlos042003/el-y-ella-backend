package com.elyella.backend.repository;

import com.elyella.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Category.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Verifica si ya existe una categoría con el nombre dado. */
    boolean existsByName(String name);
}
