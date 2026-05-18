package com.elyella.backend.controller;

import com.elyella.backend.dto.response.OrderResponse;
import com.elyella.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del historial de pedidos del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Pedidos", description = "Historial y detalle de pedidos del usuario")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Ver mis pedidos", description = "Retorna el historial de pedidos del usuario autenticado.")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.findByUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de un pedido")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.findById(id, userDetails.getUsername(), isAdmin));
    }
}
