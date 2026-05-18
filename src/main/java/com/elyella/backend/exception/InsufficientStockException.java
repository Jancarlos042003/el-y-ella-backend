package com.elyella.backend.exception;

/**
 * Lanzada cuando no hay stock suficiente para agregar un ítem al carrito.
 * Mapeada a HTTP 409 por GlobalExceptionHandler.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String flowerName, int available) {
        super("Stock insuficiente para '" + flowerName + "'. Disponible: " + available);
    }
}
