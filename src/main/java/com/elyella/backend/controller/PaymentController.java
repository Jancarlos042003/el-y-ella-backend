package com.elyella.backend.controller;

import com.elyella.backend.dto.request.CheckoutRequest;
import com.elyella.backend.dto.response.CheckoutResponse;
import com.elyella.backend.dto.response.PaymentStatusResponse;
import com.elyella.backend.service.OrderService;
import com.elyella.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de pagos: checkout con Mercado Pago y recepción de webhooks.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Checkout y gestión de pagos con Mercado Pago")
public class PaymentController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
        summary = "Iniciar checkout",
        description = """
            Reserva el stock temporalmente, crea el pedido y genera la preferencia de Mercado Pago.

            Consolidación de ítems:
            - Si se envían elementos duplicados en la lista de items (mismo flowerId), el backend los consolidará sumando sus cantidades de forma automática.

            Idempotencia:
            - Si la misma idempotencyKey ya fue procesada exitosamente, se retorna la misma respuesta.
            - Si el intento previo falló, la operación puede reintentarse con la misma clave.
            - Peticiones concurrentes con la misma clave pueden retornar HTTP 409 Conflict.
            """,
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Checkout iniciado exitosamente."
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Usuario no autenticado.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Stock insuficiente o conflicto de idempotencia.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Uno o más productos no existen.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                responseCode = "502",
                description = "Error de comunicación con Mercado Pago.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
        }
    )
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.processCheckout(userDetails.getUsername(), request));
    }

    @GetMapping("/status/{orderId}")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Consultar estado del pago de un pedido")
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    /**
     * Recibe notificaciones de pago de Mercado Pago (webhook).
     * Endpoint público — la autenticidad se verifica mediante HMAC-SHA256.
     * MP envía: POST /webhook?data.id=<paymentId>&type=payment
     */
    @PostMapping("/webhook")
    @Operation(summary = "Webhook de Mercado Pago", description = "Uso exclusivo de Mercado Pago.")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(required = false) String type,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId) {

        if ("payment".equals(type) && dataId != null && xSignature != null) {
            try {
                paymentService.processWebhook(dataId, xRequestId, xSignature);
            } catch (Exception e) {
                // 200 OK significa: "recibí la notificación". Siempre se responde 200 a MP para evitar reintentos indefinidos.
                log.error("Error inesperado al procesar webhook MP dataId={}: {}", dataId, e.getMessage(), e);
            }
        } else {
            log.debug("Notificación MP ignorada: type={}, dataId={}", type, dataId);
        }

        return ResponseEntity.ok().build();
    }
}
