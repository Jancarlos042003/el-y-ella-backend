package com.elyella.backend.service;

import com.elyella.backend.dto.request.CartItemRequest;
import com.elyella.backend.dto.response.CartResponse;
import com.elyella.backend.exception.InsufficientStockException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Cart;
import com.elyella.backend.model.Flower;
import com.elyella.backend.model.User;
import com.elyella.backend.repository.CartRepository;
import com.elyella.backend.repository.FlowerRepository;
import com.elyella.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para la gestión del carrito de compras.
 * Valida stock disponible antes de agregar o actualizar ítems.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;

    private CartResponse toResponse(Cart cart) {
        BigDecimal subtotal = cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
        return new CartResponse(
                cart.getId(),
                cart.getFlower().getId(),
                cart.getFlower().getName(),
                cart.getFlower().getImageUrl(),
                cart.getQuantity(),
                cart.getPrice(),
                subtotal,
                cart.getAddedAt()
        );
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    /** Retorna todos los ítems del carrito del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<CartResponse> getCart(String email) {
        User user = loadUser(email);
        return cartRepository.findByUserId(user.getId()).stream().map(this::toResponse).toList();
    }

    /**
     * Agrega una flor al carrito. Si ya existe, suma la cantidad solicitada.
     * Lanza InsufficientStockException si la cantidad supera el stock disponible.
     */
    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {
        User user = loadUser(email);
        Flower flower = flowerRepository.findById(request.flowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Flor", request.flowerId()));

        if (flower.getStock() < request.quantity()) {
            throw new InsufficientStockException(flower.getName(), flower.getStock());
        }

        Cart cart = cartRepository.findByUserIdAndFlowerId(user.getId(), flower.getId())
                .map(existing -> {
                    int newQty = existing.getQuantity() + request.quantity();
                    if (flower.getStock() < newQty) {
                        throw new InsufficientStockException(flower.getName(), flower.getStock());
                    }
                    existing.setQuantity(newQty);
                    return existing;
                })
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .flower(flower)
                        .quantity(request.quantity())
                        .price(flower.getPrice())
                        .build());

        return toResponse(cartRepository.save(cart));
    }

    /**
     * Reemplaza la cantidad de un ítem existente en el carrito.
     * Solo el propietario del ítem puede modificarlo.
     */
    @Transactional
    public CartResponse updateItem(Long cartItemId, String email, CartItemRequest request) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem del carrito", cartItemId));

        if (!cart.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Ítem del carrito", cartItemId);
        }

        Flower flower = cart.getFlower();
        if (flower.getStock() < request.quantity()) {
            throw new InsufficientStockException(flower.getName(), flower.getStock());
        }

        cart.setQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    /** Elimina un ítem del carrito. Solo el propietario puede eliminarlo. */
    @Transactional
    public void removeItem(Long cartItemId, String email) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem del carrito", cartItemId));

        if (!cart.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Ítem del carrito", cartItemId);
        }

        cartRepository.delete(cart);
        log.debug("Ítem del carrito eliminado: id={}", cartItemId);
    }
}
