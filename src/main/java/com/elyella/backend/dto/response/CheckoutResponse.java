package com.elyella.backend.dto.response;

/**
 * Respuesta del proceso de checkout.
 * El frontend usa initPoint para redirigir al usuario a Mercado Pago.
 */
public record CheckoutResponse(
        Long orderId,
        Long paymentId,
        String initPoint
) {}
