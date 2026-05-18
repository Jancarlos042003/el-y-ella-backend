package com.elyella.backend.repository;

import com.elyella.backend.model.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Flower.
 * Extiende JpaSpecificationExecutor para soportar búsqueda dinámica con Specification.
 */
@Repository
public interface FlowerRepository extends JpaRepository<Flower, Long>, JpaSpecificationExecutor<Flower> {

    /** Retorna todas las flores que pertenecen a una categoría específica. */
    @Query("SELECT f FROM Flower f JOIN f.categories c WHERE c.id = :categoryId")
    List<Flower> findByCategoryId(@Param("categoryId") Long categoryId);

    /** Retorna flores con stock disponible mayor a cero. */
    List<Flower> findByStockGreaterThan(int stock);
}
