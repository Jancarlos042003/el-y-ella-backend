package com.elyella.backend.dto.response;

import com.elyella.backend.model.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del proceso de checkout.
 * El frontend usa initPoint para redirigir al usuario a Mercado Pago.
 */
public record CheckoutResponse(
        Long orderId,
        Long paymentId,
        @Schema(format = "uri", description = "Enlace de redirección a Mercado Pago para realizar el pago.")
        String initPoint,
        PaymentStatus status
) {}
