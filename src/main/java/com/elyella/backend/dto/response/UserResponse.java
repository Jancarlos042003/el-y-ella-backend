package com.elyella.backend.dto.response;

import com.elyella.backend.model.enums.Role;

import java.time.LocalDateTime;

/**
 * Representación pública de un usuario (sin contraseña).
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String address,
        String phone,
        Role role,
        LocalDateTime createdAt
) {}
