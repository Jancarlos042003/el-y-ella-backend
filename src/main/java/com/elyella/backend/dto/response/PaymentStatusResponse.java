package com.elyella.backend.dto.response;

import com.elyella.backend.model.enums.PaymentStatus;

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
        String mpPaymentId,
        LocalDateTime createdAt
) {}
