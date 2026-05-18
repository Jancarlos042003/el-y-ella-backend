package com.elyella.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiscountResponse(
        Long id,
        Long flowerId,
        String flowerName,
        BigDecimal discountPercentage,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        boolean active
) {}
