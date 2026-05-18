package com.elyella.backend.model.enums;

/**
 * Estados posibles de un pago procesado por Mercado Pago.
 */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
