package com.elyella.backend.exception;

/**
 * Lanzada cuando los datos de un checkout no se pueden procesar semánticamente
 * (ej. flor no existe, cantidades inválidas). Mapeada a HTTP 422.
 */
public class InvalidCheckoutException extends RuntimeException {
    
    public InvalidCheckoutException(String message) {
        super(message);
    }
}
