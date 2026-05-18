package com.elyella.backend.service;

import com.elyella.backend.dto.request.CheckoutRequest;
import com.elyella.backend.dto.request.UpdateOrderStatusRequest;
import com.elyella.backend.dto.response.CheckoutResponse;
import com.elyella.backend.dto.response.OrderDetailResponse;
import com.elyella.backend.dto.response.OrderResponse;
import com.elyella.backend.exception.InsufficientStockException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Cart;
import com.elyella.backend.model.Order;
import com.elyella.backend.model.OrderDetail;
import com.elyella.backend.model.Payment;
import com.elyella.backend.model.User;
import com.elyella.backend.model.enums.PaymentStatus;
import com.elyella.backend.repository.CartRepository;
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
import java.util.List;
import java.util.Map;

/**
 * Servicio para la gestión de pedidos y el flujo de checkout.
 * El método processCheckout() replica el flujo ACID del legado PagoDAO.procesarPagoYPedido().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

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
     * Procesa el checkout del usuario: valida el carrito, crea el pedido,
     * descuenta el stock, limpia el carrito y genera la preferencia de Mercado Pago.
     *
     * Flujo ACID (replica PagoDAO.procesarPagoYPedido() del legado):
     * 1. Validar carrito no vacío y stock suficiente.
     * 2. Calcular total.
     * 3. Crear Order (PENDING) con sus OrderDetail.
     * 4. Decrementar stock de cada flor.
     * 5. Crear Payment (PENDING) con datos de envío.
     * 6. Vaciar carrito.
     * 7. Crear preferencia en Mercado Pago.
     * 8. Actualizar Payment.mpPreferenceId.
     * 9. Retornar CheckoutResponse con initPoint para redirigir al usuario.
     *
     * Si cualquier paso falla → rollback automático por @Transactional.
     */
    @Transactional
    public CheckoutResponse processCheckout(String email, CheckoutRequest request) {
        User user = loadUser(email);
        List<Cart> cartItems = cartRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new ResourceNotFoundException("El carrito del usuario está vacío.");
        }

        // 1. Validar stock para todos los ítems antes de proceder
        for (Cart item : cartItems) {
            if (item.getFlower().getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        item.getFlower().getName(), item.getFlower().getStock()
                );
            }
        }

        // 2. Calcular total
        BigDecimal total = cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Crear Order con sus OrderDetail (precio snapshot del carrito)
        Order order = Order.builder()
                .user(user)
                .total(total)
                .build();

        List<OrderDetail> details = cartItems.stream()
                .map(item -> OrderDetail.builder()
                        .order(order)
                        .flower(item.getFlower())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        order.getDetails().addAll(details);
        orderRepository.save(order);

        // 4. Decrementar stock
        cartItems.forEach(item -> {
            var flower = item.getFlower();
            flower.setStock(flower.getStock() - item.getQuantity());
            flowerRepository.save(flower);
        });

        // 5. Crear Payment (PENDING) con datos de envío
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

        // 6. Vaciar carrito
        cartRepository.deleteByUserId(user.getId());

        // 7. Crear preferencia en Mercado Pago (llamada externa — si falla, @Transactional hace rollback)
        Map<String, String> mpResult = paymentService.createPreference(order);

        // 8. Actualizar Payment con el ID de preferencia MP
        payment.setMpPreferenceId(mpResult.get("preferenceId"));
        paymentRepository.save(payment);

        log.info("Checkout completado para usuario={}, pedido id={}, preferencia MP={}",
                email, order.getId(), payment.getMpPreferenceId());

        return new CheckoutResponse(order.getId(), payment.getId(), mpResult.get("initPoint"));
    }
}
