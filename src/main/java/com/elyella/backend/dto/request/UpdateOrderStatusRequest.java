package com.elyella.backend.dto.request;

import com.elyella.backend.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para actualizar el estado de un pedido (uso exclusivo del administrador).
 */
public record UpdateOrderStatusRequest(

        @NotNull(message = "El estado del pedido es obligatorio")
        OrderStatus status
) {}
