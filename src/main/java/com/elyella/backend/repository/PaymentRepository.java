package com.elyella.backend.repository;

import com.elyella.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Payment.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Busca un pago por el ID de preferencia de Mercado Pago (recibido al crear la preferencia). */
    Optional<Payment> findByMpPreferenceId(String mpPreferenceId);

    /** Busca un pago por el ID de pago de Mercado Pago (recibido vía webhook). */
    Optional<Payment> findByMpPaymentId(String mpPaymentId);

    /** Retorna el pago asociado a un pedido específico. */
    Optional<Payment> findByOrderId(Long orderId);
}
