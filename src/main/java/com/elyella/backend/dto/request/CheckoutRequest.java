package com.elyella.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos de envío requeridos para procesar el checkout.
 * Los datos de pago son gestionados por Mercado Pago — no se reciben aquí.
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

        String postalCode
) {}
