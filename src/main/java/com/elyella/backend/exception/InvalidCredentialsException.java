package com.elyella.backend.exception;

/**
 * Lanzada cuando las credenciales de autenticación son incorrectas.
 * Mapeada a HTTP 401 por GlobalExceptionHandler.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
