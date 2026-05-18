package com.elyella.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Datos para crear o actualizar un arreglo floral.
 */
public record FlowerRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal price,

        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock,

        @Schema(description = "URL pública de la imagen. Obtenerla previamente desde POST /api/v1/images/upload.")
        String imageUrl,

        Set<Long> categoryIds
) {}
