package com.elyella.backend.dto.response;

import java.math.BigDecimal;

/**
 * Representación de un ítem dentro de un pedido.
 */
public record OrderDetailResponse(
        Long id,
        Long flowerId,
        String flowerName,
        String flowerImageUrl,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}
