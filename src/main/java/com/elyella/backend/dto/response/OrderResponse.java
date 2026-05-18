package com.elyella.backend.dto.response;

import com.elyella.backend.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación completa de un pedido con sus detalles.
 */
public record OrderResponse(
        Long id,
        Long userId,
        String userName,
        BigDecimal total,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderDetailResponse> details
) {}
