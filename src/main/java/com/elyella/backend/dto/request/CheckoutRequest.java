package com.elyella.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Datos requeridos para procesar el checkout.
 */
public record CheckoutRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        String phone,

        @NotBlank(message = "La dirección de envío es obligatoria")
        String shippingAddress,

        String city,

        String state,

        String postalCode,
        
        @NotEmpty(message = "El pedido debe contener al menos un ítem")
        @Valid
        List<CheckoutItemRequest> items,
        
        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String idempotencyKey
) {}
