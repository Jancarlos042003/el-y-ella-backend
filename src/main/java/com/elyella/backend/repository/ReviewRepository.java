package com.elyella.backend.repository;

import com.elyella.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Review.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Retorna todas las reseñas de una flor específica ordenadas por fecha descendente. */
    List<Review> findByFlowerIdOrderByCreatedAtDesc(Long flowerId);

    /** Retorna todas las reseñas escritas por un usuario. */
    List<Review> findByUserId(Long userId);

    /** Verifica si un usuario ya dejó reseña sobre una flor específica. */
    boolean existsByFlowerIdAndUserId(Long flowerId, Long userId);
}
