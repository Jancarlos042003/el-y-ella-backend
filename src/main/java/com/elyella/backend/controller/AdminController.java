package com.elyella.backend.controller;

import com.elyella.backend.dto.request.UpdateOrderStatusRequest;
import com.elyella.backend.dto.request.UpdateUserRequest;
import com.elyella.backend.dto.response.OrderResponse;
import com.elyella.backend.dto.response.UserResponse;
import com.elyella.backend.service.OrderService;
import com.elyella.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de administración: gestión de usuarios y pedidos.
 * Acceso restringido a usuarios con rol ADMIN (configurado en SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Administración", description = "Gestión de usuarios y pedidos — solo ADMIN")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;

    // ── Usuarios ─────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Ver detalle de un usuario")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Actualizar datos de un usuario")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Eliminar usuario")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Pedidos ──────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(summary = "Listar todos los pedidos")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Cambiar estado de un pedido")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}
