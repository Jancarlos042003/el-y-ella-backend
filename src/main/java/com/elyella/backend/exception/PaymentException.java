package com.elyella.backend.exception;

/**
 * Lanzada cuando ocurre un error al comunicarse con Mercado Pago.
 * Mapeada a HTTP 502 por GlobalExceptionHandler.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
