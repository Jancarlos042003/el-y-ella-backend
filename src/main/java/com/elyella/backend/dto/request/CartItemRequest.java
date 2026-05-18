package com.elyella.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para agregar o actualizar un ítem en el carrito.
 */
public record CartItemRequest(

        @NotNull(message = "El id de la flor es obligatorio")
        Long flowerId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity
) {}
