package com.elyella.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountRequest(

        @NotNull(message = "El id de la flor es obligatorio")
        Long flowerId,

        @NotNull(message = "El porcentaje de descuento es obligatorio")
        @DecimalMin(value = "1.00", message = "El descuento mínimo es 1%")
        @DecimalMax(value = "100.00", message = "El descuento máximo es 100%")
        BigDecimal discountPercentage,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @Schema(description = "Fecha de fin del descuento. Null = sin expiración.")
        LocalDate endDate
) {}
