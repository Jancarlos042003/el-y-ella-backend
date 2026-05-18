package com.elyella.backend.service;

import com.elyella.backend.dto.response.PaymentStatusResponse;
import com.elyella.backend.exception.PaymentException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Order;
import com.elyella.backend.model.Payment;
import com.elyella.backend.model.enums.OrderStatus;
import com.elyella.backend.model.enums.PaymentStatus;
import com.elyella.backend.repository.OrderRepository;
import com.elyella.backend.repository.PaymentRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Servicio de integración con Mercado Pago.
 * Gestiona la creación de preferencias de pago y el procesamiento de webhooks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Crea una preferencia de pago en Mercado Pago con los ítems del pedido.
     * Retorna el ID de la preferencia y el initPoint para redirigir al usuario.
     *
     * @param order el pedido con sus detalles ya persistidos
     * @return mapa con "preferenceId" e "initPoint"
     */
    public Map<String, String> createPreference(Order order) {
        try {
            List<PreferenceItemRequest> items = order.getDetails().stream()
                    .map(detail -> PreferenceItemRequest.builder()
                            .id(String.valueOf(detail.getFlower().getId()))
                            .title(detail.getFlower().getName())
                            .quantity(detail.getQuantity())
                            .unitPrice(detail.getPrice())
                            .build())
                    .toList();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .externalReference(String.valueOf(order.getId()))
                    .notificationUrl(notificationUrl)
                    .build();

            Preference preference = new PreferenceClient().create(preferenceRequest);
            log.debug("Preferencia MP creada: id={} para pedido id={}", preference.getId(), order.getId());
            return Map.of(
                    "preferenceId", preference.getId(),
                    "initPoint", preference.getInitPoint()
            );
        } catch (MPException | MPApiException e) {
            throw new PaymentException("Error al crear preferencia en Mercado Pago: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna el estado actual del pago asociado a un pedido.
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago para el pedido", orderId));
        return new PaymentStatusResponse(
                payment.getId(), orderId, payment.getPaymentStatus(),
                payment.getTotalAmount(), payment.getMpPaymentId(), payment.getCreatedAt()
        );
    }

    /**
     * Procesa una notificación de Mercado Pago (webhook).
     * Verifica la firma HMAC-SHA256 y actualiza el estado del pago y pedido.
     *
     * @param dataId       ID del pago en Mercado Pago (query param data.id)
     * @param xRequestId   header X-Request-Id de la notificación
     * @param xSignature   header X-Signature con ts y v1
     */
    @Transactional
    public void processWebhook(String dataId, String xRequestId, String xSignature) {
        if (!verifySignature(dataId, xRequestId, xSignature)) {
            log.warn("Firma de webhook inválida para dataId={}", dataId);
            throw new PaymentException("Firma del webhook de Mercado Pago no válida.");
        }

        try {
            Long mpPaymentId = Long.parseLong(dataId);
            com.mercadopago.resources.payment.Payment mpPayment = new PaymentClient().get(mpPaymentId);

            String externalRef = mpPayment.getExternalReference();
            if (externalRef == null || externalRef.isBlank()) {
                log.warn("Webhook sin externalReference para mpPaymentId={}", mpPaymentId);
                return;
            }

            Long orderId = Long.parseLong(externalRef);
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseGet(() -> {
                        log.warn("No se encontró pago local para pedido id={}", orderId);
                        return null;
                    });

            if (payment == null) return;

            payment.setMpPaymentId(dataId);
            Order order = payment.getOrder();

            switch (mpPayment.getStatus()) {
                case "approved" -> {
                    payment.setPaymentStatus(PaymentStatus.APPROVED);
                    order.setStatus(OrderStatus.SHIPPED);
                    log.info("Pago aprobado para pedido id={}", orderId);
                }
                case "rejected" -> {
                    payment.setPaymentStatus(PaymentStatus.REJECTED);
                    log.info("Pago rechazado para pedido id={}", orderId);
                }
                case "cancelled" -> {
                    payment.setPaymentStatus(PaymentStatus.CANCELLED);
                    order.setStatus(OrderStatus.CANCELLED);
                    log.info("Pago cancelado para pedido id={}", orderId);
                }
                default -> log.debug("Estado MP ignorado '{}' para pedido id={}", mpPayment.getStatus(), orderId);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

        } catch (NumberFormatException e) {
            log.error("dataId inválido en webhook: {}", dataId);
        } catch (MPApiException e) {
            // Pago no encontrado en MP (ej. ID de prueba) u otro error de API — se registra y se ignora.
            log.error("Error de API de Mercado Pago al consultar pago id={}: status={} mensaje={}",
                    dataId, e.getStatusCode(), e.getMessage());
        } catch (MPException e) {
            log.error("Error de red al consultar pago id={} en Mercado Pago: {}", dataId, e.getMessage());
        }
    }

    /**
     * Verifica la firma HMAC-SHA256 del webhook de Mercado Pago.
     * Formato de x-signature: ts=<unix_ts>,v1=<hex_hash>
     * Payload firmado: id:<dataId>;request-id:<xRequestId>;ts:<ts>;
     */
    private boolean verifySignature(String dataId, String xRequestId, String xSignature) {
        try {
            Map<String, String> parts = new HashMap<>();
            for (String part : xSignature.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) parts.put(kv[0].trim(), kv[1].trim());
            }

            String ts = parts.get("ts");
            String v1 = parts.get("v1");
            if (ts == null || v1 == null) return false;

            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    v1.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error al verificar firma del webhook: {}", e.getMessage());
            return false;
        }
    }
}
