package com.elyella.backend.dto.response;

import com.elyella.backend.model.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado actual de un pago asociado a un pedido.
 */
public record PaymentStatusResponse(
        Long paymentId,
        Long orderId,
        PaymentStatus status,
        BigDecimal totalAmount,
        @Schema(nullable = true, description = "ID del pago retornado por Mercado Pago. Solo disponible una vez que la pasarela notifica el pago aprobado/rechazado.")
        String mpPaymentId,
        LocalDateTime createdAt
) {}
