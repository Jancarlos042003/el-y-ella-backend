package com.elyella.backend.scheduler;

import com.elyella.backend.model.Order;
import com.elyella.backend.model.enums.OrderStatus;
import com.elyella.backend.model.enums.PaymentStatus;
import com.elyella.backend.repository.CartRepository;
import com.elyella.backend.repository.FlowerRepository;
import com.elyella.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tareas programadas de mantenimiento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final FlowerRepository flowerRepository;

    /**
     * Elimina los carritos con más de 30 días de antigüedad.
     * Se ejecuta todos los días a las 02:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanAbandonedCarts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = cartRepository.deleteByAddedAtBefore(cutoff);
        log.info("Limpieza de carritos abandonados completada: {} registros eliminados.", deleted);
    }

    /**
     * Busca pedidos en estado RESERVED cuyo tiempo de expiración ya pasó.
     * Libera el stock y marca la orden y el pago como EXPIRED.
     * Se ejecuta cada 5 minutos (300,000 milisegundos).
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void expireReservations() {
        log.debug("Ejecutando job de expiración de reservas...");
        List<Order> expiredOrders = orderRepository.findByStatusAndReservationExpiresAtBefore(
                OrderStatus.RESERVED, LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            log.info("Expirando pedido id={} y revirtiendo stock", order.getId());
            
            // Revertir el stock
            order.getDetails().forEach(detail -> {
                var flower = detail.getFlower();
                flower.setStock(flower.getStock() + detail.getQuantity());
                flowerRepository.save(flower);
            });
            
            // Actualizar estados
            order.setStatus(OrderStatus.EXPIRED);
            order.setReservationExpiresAt(null);
            
            if (order.getPayment() != null) {
                order.getPayment().setPaymentStatus(PaymentStatus.EXPIRED);
            }
        }
        
        if (!expiredOrders.isEmpty()) {
            orderRepository.saveAll(expiredOrders);
            log.info("Job finalizado: {} reservas expiradas.", expiredOrders.size());
        }
    }
}
