package com.elyella.backend.exception;

/**
 * Lanzada cuando se intenta crear un recurso que ya existe (ej: email duplicado).
 * Mapeada a HTTP 409 por GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
