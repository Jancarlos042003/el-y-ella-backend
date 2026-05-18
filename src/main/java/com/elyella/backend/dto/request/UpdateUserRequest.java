package com.elyella.backend.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales para actualizar el perfil de un usuario.
 */
public record UpdateUserRequest(

        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        String address,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone
) {}
