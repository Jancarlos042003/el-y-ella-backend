package com.elyella.backend.dto.response;

import java.time.LocalDateTime;

/**
 * Representación pública de una reseña de producto.
 */
public record ReviewResponse(
        Long id,
        Long flowerId,
        String flowerName,
        Long userId,
        String userName,
        String comment,
        int rating,
        LocalDateTime createdAt
) {}
