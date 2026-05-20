package com.elyella.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutItemRequest(
        @NotNull(message = "El ID de la flor es obligatorio")
        Long flowerId,

        @Positive(message = "La cantidad debe ser mayor a 0")
        @NotNull(message = "La cantidad es obligatoria")
        Integer quantity
) {}
