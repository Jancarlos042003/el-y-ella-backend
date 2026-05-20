package com.elyella.backend.service;

import com.elyella.backend.dto.request.CheckoutItemRequest;
import com.elyella.backend.dto.request.CheckoutRequest;
import com.elyella.backend.dto.request.UpdateOrderStatusRequest;
import com.elyella.backend.dto.response.CheckoutResponse;
import com.elyella.backend.dto.response.OrderDetailResponse;
import com.elyella.backend.dto.response.OrderResponse;
import com.elyella.backend.exception.InsufficientStockException;
import com.elyella.backend.exception.InvalidCheckoutException;
import com.elyella.backend.exception.PaymentException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.CheckoutAttempt;
import com.elyella.backend.model.Flower;
import com.elyella.backend.model.Order;
import com.elyella.backend.model.OrderDetail;
import com.elyella.backend.model.Payment;
import com.elyella.backend.model.User;
import com.elyella.backend.model.enums.CheckoutStatus;
import com.elyella.backend.model.enums.OrderStatus;
import com.elyella.backend.model.enums.PaymentStatus;
import com.elyella.backend.repository.CheckoutAttemptRepository;
import com.elyella.backend.repository.FlowerRepository;
import com.elyella.backend.repository.OrderRepository;
import com.elyella.backend.repository.PaymentRepository;
import com.elyella.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de pedidos y el flujo de checkout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final CheckoutAttemptRepository checkoutAttemptRepository;

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    private OrderDetailResponse toDetailResponse(OrderDetail d) {
        BigDecimal subtotal = d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
        return new OrderDetailResponse(
                d.getId(), d.getFlower().getId(), d.getFlower().getName(),
                d.getFlower().getImageUrl(), d.getQuantity(), d.getPrice(), subtotal
        );
    }

    private OrderResponse toResponse(Order order) {
        List<OrderDetailResponse> details = order.getDetails().stream()
                .map(this::toDetailResponse).toList();
        return new OrderResponse(
                order.getId(), order.getUser().getId(), order.getUser().getName(),
                order.getTotal(), order.getStatus(), order.getCreatedAt(), details
        );
    }

    /** Retorna el historial de pedidos del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findByUser(String email) {
        User user = loadUser(email);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    /** Retorna el detalle de un pedido. Solo el propietario o un ADMIN puede consultarlo. */
    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, String email, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Pedido", id);
        }
        return toResponse(order);
    }

    /** Retorna todos los pedidos (solo ADMIN). */
    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Actualiza el estado de un pedido (solo ADMIN).
     */
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
        order.setStatus(request.status());
        log.info("Estado del pedido id={} actualizado a {}", id, request.status());
        return toResponse(order);
    }

    /**
     * Procesa el checkout de manera segura:
     * 1. Valida Idempotencia.
     * 2. Consolida ítems y lee de BD.
     * 3. Valida y reserva stock.
     * 4. Crea Order (RESERVED) y Payment (PENDING).
     * 5. Intenta conectar con Mercado Pago. Si falla, compensa revirtiendo el stock.
     */
    @Transactional
    public CheckoutResponse processCheckout(String email, CheckoutRequest request) {
        User user = loadUser(email);

        // 1. Validar Idempotencia
        Optional<CheckoutAttempt> attempt = checkoutAttemptRepository.findByUserIdAndIdempotencyKey(user.getId(), request.idempotencyKey());
        if (attempt.isPresent() && attempt.get().isSuccess()) {
            log.info("Intento duplicado exitoso detectado para idempotencyKey={}", request.idempotencyKey());
            Order existingOrder = attempt.get().getOrder();
            Payment existingPayment = attempt.get().getPayment();
            return new CheckoutResponse(existingOrder.getId(), existingPayment.getId(), existingPayment.getMpPreferenceId(), "PENDING");
        }

        // 2. Consolidar items y cargar productos de BD
        Map<Long, Integer> itemQuantities = request.items().stream()
                .collect(Collectors.toMap(
                        CheckoutItemRequest::flowerId,
                        CheckoutItemRequest::quantity,
                        Integer::sum // Suma si hay repetidos
                ));

        List<Flower> flowers = flowerRepository.findAllById(itemQuantities.keySet());
        if (flowers.size() != itemQuantities.size()) {
            throw new InvalidCheckoutException("Uno o más productos no existen en la base de datos.");
        }

        // 3. Validar stock
        for (Flower flower : flowers) {
            int quantity = itemQuantities.get(flower.getId());
            if (flower.getStock() < quantity) {
                throw new InsufficientStockException(flower.getName(), flower.getStock());
            }
        }

        // 4. Calcular total con precios de BD y crear Order (RESERVED)
        BigDecimal total = flowers.stream()
                .map(f -> f.getPrice().multiply(BigDecimal.valueOf(itemQuantities.get(f.getId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .status(OrderStatus.RESERVED) // Reservado por 15 min
                .reservationExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        List<OrderDetail> details = flowers.stream()
                .map(flower -> OrderDetail.builder()
                        .order(order)
                        .flower(flower)
                        .quantity(itemQuantities.get(flower.getId()))
                        .price(flower.getPrice())
                        .build())
                .toList();

        order.getDetails().addAll(details);
        orderRepository.save(order);

        // 5. Descontar Stock inmediatamente (Reserva)
        for (Flower flower : flowers) {
            flower.setStock(flower.getStock() - itemQuantities.get(flower.getId()));
            flowerRepository.save(flower);
        }

        // 6. Crear Payment (PENDING)
        Payment payment = Payment.builder()
                .user(user)
                .order(order)
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .shippingAddress(request.shippingAddress())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .totalAmount(total)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // 7. Llamada a Mercado Pago con try-catch para compensación manual
        try {
            Map<String, String> mpResult = paymentService.createPreference(order);
            
            // Actualizamos Payment y guardamos intento exitoso
            payment.setMpPreferenceId(mpResult.get("preferenceId"));
            paymentRepository.save(payment); // Solo para forzar guardado y evitar problemas al enviar MP el preferenceId si falla el attempt
            
            // La preference que devolvemos en initPoint se envía, pero en BD guardamos la preference de MP
            saveCheckoutAttempt(user, request.idempotencyKey(), order, payment, CheckoutStatus.SUCCESS);

            log.info("Checkout completado para usuario={}, pedido id={}, idempotencyKey={}",
                    email, order.getId(), request.idempotencyKey());

            return new CheckoutResponse(order.getId(), payment.getId(), mpResult.get("initPoint"), "PENDING");

        } catch (Exception e) {
            // Compensación manual: Revertir stock
            log.error("Error al crear preferencia en Mercado Pago, revirtiendo stock para pedido id={}", order.getId(), e);
            for (Flower flower : flowers) {
                flower.setStock(flower.getStock() + itemQuantities.get(flower.getId()));
                flowerRepository.save(flower);
            }
            
            // Actualizar estados a Fallidos
            order.setStatus(OrderStatus.CANCELLED);
            order.setReservationExpiresAt(null);
            orderRepository.save(order);
            
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            saveCheckoutAttempt(user, request.idempotencyKey(), null, null, CheckoutStatus.FAILED);
            
            throw new PaymentException("Falla al comunicar con la pasarela de pagos. El pedido ha sido cancelado y el stock liberado.");
        }
    }
    
    private void saveCheckoutAttempt(User user, String idempotencyKey, Order order, Payment payment, CheckoutStatus status) {
        CheckoutAttempt attempt = checkoutAttemptRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                .orElseGet(() -> CheckoutAttempt.builder()
                        .user(user)
                        .idempotencyKey(idempotencyKey)
                        .build());

        attempt.setOrder(order);
        attempt.setPayment(payment);
        attempt.setStatus(status);

        checkoutAttemptRepository.save(attempt);
    }
}