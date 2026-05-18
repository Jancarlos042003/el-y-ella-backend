package com.elyella.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representación de un ítem en el carrito de compras.
 */
public record CartResponse(
        Long id,
        Long flowerId,
        String flowerName,
        String flowerImageUrl,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal,
        LocalDateTime addedAt
) {}
