package com.elyella.backend.controller;

import com.elyella.backend.dto.request.CartItemRequest;
import com.elyella.backend.dto.response.CartResponse;
import com.elyella.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del carrito de compras del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Carrito", description = "Gestión del carrito de compras")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Ver mi carrito")
    public ResponseEntity<List<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(userDetails.getUsername()));
    }

    @PostMapping
    @Operation(summary = "Agregar ítem al carrito", description = "Si la flor ya existe en el carrito, suma la cantidad.")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserDetails userDetails,
                                                @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cantidad de un ítem")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails,
                                                   @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(id, userDetails.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar ítem del carrito")
    public ResponseEntity<Void> removeItem(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        cartService.removeItem(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
