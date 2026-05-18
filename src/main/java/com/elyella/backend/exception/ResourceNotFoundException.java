package com.elyella.backend.exception;

/**
 * Lanzada cuando un recurso solicitado no existe en la base de datos.
 * Mapeada a HTTP 404 por GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " no encontrado con id: " + id);
    }
}
