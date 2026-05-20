package com.elyella.backend.model.enums;

/**
 * Estados posibles de un pedido en el sistema.
 */
public enum OrderStatus {
    PENDING,
    RESERVED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    EXPIRED
}
