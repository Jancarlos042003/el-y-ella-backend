package com.elyella.backend.dto.response;

/**
 * Información del usuario devuelta tras autenticarse o registrarse.
 * El JWT ya no se incluye en el body — se envía como cookie HTTP-only.
 */
public record AuthResponse(
        String name,
        String email,
        String role
) {}
