package com.elyella.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Representación pública de un arreglo floral con sus categorías.
 */
public record FlowerResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        Set<CategoryResponse> categories,
        LocalDateTime createdAt
) {}
