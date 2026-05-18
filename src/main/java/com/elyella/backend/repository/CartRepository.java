package com.elyella.backend.repository;

import com.elyella.backend.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Cart.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /** Retorna todos los ítems del carrito de un usuario. */
    List<Cart> findByUserId(Long userId);

    /** Busca un ítem específico del carrito por usuario y flor (para evitar duplicados). */
    Optional<Cart> findByUserIdAndFlowerId(Long userId, Long flowerId);

    /** Elimina todos los ítems del carrito de un usuario (usado en el checkout). */
    void deleteByUserId(Long userId);

    /** Verifica si existe algún ítem en el carrito para un usuario dado. */
    boolean existsByUserId(Long userId);

    /** Elimina en bloque los carritos abandonados anteriores a la fecha indicada. */
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.addedAt < :cutoff")
    int deleteByAddedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
