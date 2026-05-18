package com.elyella.backend.repository;

import com.elyella.backend.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad OrderDetail.
 */
@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    /** Retorna todos los ítems de un pedido específico. */
    List<OrderDetail> findByOrderId(Long orderId);
}
