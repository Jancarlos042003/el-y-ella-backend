package com.elyella.backend.repository;

import com.elyella.backend.model.Order;
import com.elyella.backend.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Order.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Retorna todos los pedidos de un usuario ordenados por fecha descendente. */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Retorna todos los pedidos filtrados por estado (útil para el panel admin). */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
