package com.elyella.backend.controller;

import com.elyella.backend.dto.request.DiscountRequest;
import com.elyella.backend.dto.response.DiscountResponse;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Tag(name = "Descuentos", description = "Gestión de descuentos en arreglos florales")
public class DiscountController {

    private final DiscountService discountService;

    // ── Endpoints públicos ────────────────────────────────────────────────────

    @GetMapping("/active")
    @Operation(
            summary = "Listar descuentos activos vigentes hoy",
            description = "Retorna todos los descuentos con active=true cuya fecha de inicio sea " +
                          "hoy o anterior y cuya fecha de fin sea nula o posterior a hoy."
    )
    public ResponseEntity<List<DiscountResponse>> getActive() {
        return ResponseEntity.ok(discountService.findActiveDiscounts());
    }

    @GetMapping("/flower/{flowerId}/active")
    @Operation(
            summary = "Descuento activo vigente hoy para una flor",
            description = "Retorna el descuento activo y vigente hoy para la flor indicada. " +
                          "Devuelve 404 si no existe ningún descuento activo para esa flor hoy."
    )
    public ResponseEntity<DiscountResponse> getActiveByFlower(@PathVariable Long flowerId) {
        return discountService.findActiveByFlower(flowerId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay descuento activo vigente para la flor con id: " + flowerId));
    }

    // ── Endpoints ADMIN ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Listar todos los descuentos (ADMIN)")
    public ResponseEntity<List<DiscountResponse>> getAll() {
        return ResponseEntity.ok(discountService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Obtener descuento por ID (ADMIN)")
    public ResponseEntity<DiscountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(discountService.findById(id));
    }

    @GetMapping("/flower/{flowerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Listar todos los descuentos de una flor (ADMIN)",
            description = "Retorna el historial completo de descuentos (activos e inactivos) para la flor indicada."
    )
    public ResponseEntity<List<DiscountResponse>> getByFlower(@PathVariable Long flowerId) {
        return ResponseEntity.ok(discountService.findByFlower(flowerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Crear descuento (ADMIN)",
            description = "Crea un descuento para una flor. No se permiten dos descuentos activos " +
                          "con fechas solapadas para la misma flor — se retornará 409 en ese caso."
    )
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Actualizar descuento (ADMIN)",
            description = "Actualiza todos los campos del descuento. " +
                          "Aplica la misma validación de solapamiento que al crear."
    )
    public ResponseEntity<DiscountResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(discountService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Desactivar descuento (ADMIN)",
            description = "Establece active=false sin eliminar el registro. " +
                          "Útil para pausar un descuento manualmente sin modificar fechas."
    )
    public ResponseEntity<DiscountResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(discountService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Eliminar descuento (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
