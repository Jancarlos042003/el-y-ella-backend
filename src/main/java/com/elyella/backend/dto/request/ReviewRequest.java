package com.elyella.backend.dto.request;

import jakarta.validation.constraints.*;

/**
 * Datos para crear o actualizar una reseña.
 */
public record ReviewRequest(

        @NotNull(message = "El id de la flor es obligatorio")
        Long flowerId,

        @NotBlank(message = "El comentario es obligatorio")
        String comment,

        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        int rating
) {}
