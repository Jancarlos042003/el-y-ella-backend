package com.elyella.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones. Retorna respuestas en formato RFC 9457 (ProblemDetail).
 * Extiende ResponseEntityExceptionHandler para reutilizar el manejo nativo de Spring MVC.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE_TYPE = "https://api.elyella.com/errors/";

    // ── Excepciones de dominio ────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso no encontrado");
        pd.setType(URI.create(BASE_TYPE + "not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex) {
        log.debug("Recurso duplicado: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Recurso duplicado");
        pd.setType(URI.create(BASE_TYPE + "duplicate"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.debug("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "No se puede eliminar o modificar el recurso porque está siendo referenciado por otros registros.");
        pd.setTitle("Conflicto de integridad");
        pd.setType(URI.create(BASE_TYPE + "data-integrity"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleStock(InsufficientStockException ex) {
        log.debug("Stock insuficiente: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Stock insuficiente");
        pd.setType(URI.create(BASE_TYPE + "insufficient-stock"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Credenciales inválidas");
        pd.setType(URI.create(BASE_TYPE + "unauthorized"));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ProblemDetail> handlePayment(PaymentException ex) {
        log.error("Error con Mercado Pago: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        pd.setTitle("Error en el servicio de pago");
        pd.setType(URI.create(BASE_TYPE + "payment-error"));
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }

    // ── Excepciones de Spring Security ───────────────────────────────────────

    /**
     * Lanzada por AuthenticationManager cuando las credenciales son incorrectas.
     * Se captura aquí porque se origina dentro del controlador, no en el filtro de seguridad.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleAuthFailure(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Correo o contraseña incorrectos");
        pd.setTitle("Credenciales inválidas");
        pd.setType(URI.create(BASE_TYPE + "unauthorized"));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    /**
     * Lanzada por @PreAuthorize cuando el usuario no tiene el rol requerido.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "No tienes permiso para acceder a este recurso.");
        pd.setTitle("Acceso denegado");
        pd.setType(URI.create(BASE_TYPE + "forbidden"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    // ── Validación de argumentos (Bean Validation) ────────────────────────────

    /**
     * Lanzada cuando @Valid falla en un @RequestBody.
     * Se agrega la propiedad "errors" con el detalle campo por campo.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Los datos enviados contienen errores de validación.");
        pd.setTitle("Error de validación");
        pd.setType(URI.create(BASE_TYPE + "validation-error"));

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        pd.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(pd);
    }

    // ── Fallback genérico ─────────────────────────────────────────────────────

    /**
     * Captura cualquier excepción no manejada para evitar exponer detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Por favor, intenta de nuevo más tarde.");
        pd.setTitle("Error interno del servidor");
        pd.setType(URI.create(BASE_TYPE + "internal-error"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
