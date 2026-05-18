package com.elyella.backend.controller;

import com.elyella.backend.dto.request.FlowerRequest;
import com.elyella.backend.dto.response.FlowerResponse;
import com.elyella.backend.service.FlowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Endpoints del catálogo de arreglos florales.
 * Lectura pública; creación, actualización y eliminación restringidas a ADMIN.
 */
@RestController
@RequestMapping("/api/v1/flowers")
@RequiredArgsConstructor
@Tag(name = "Flores", description = "Catálogo de arreglos florales")
public class FlowerController {

    private final FlowerService flowerService;

    @GetMapping
    @Operation(
            summary = "Listar flores con paginación y filtros",
            description = "Todos los parámetros son opcionales. Sin params devuelve la primera página con los más recientes."
    )
    public ResponseEntity<Page<FlowerResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(flowerService.search(q, categoryId, sort, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener flor por ID")
    public ResponseEntity<FlowerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flowerService.findById(id));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Listar flores por categoría")
    public ResponseEntity<List<FlowerResponse>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(flowerService.findByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Crear flor (ADMIN)",
            description = "Si la flor incluye imagen, primero súbela a POST /api/v1/images/upload " +
                          "y envía la URL resultante en el campo imageUrl."
    )
    public ResponseEntity<FlowerResponse> create(@Valid @RequestBody FlowerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flowerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Actualizar flor (ADMIN)",
            description = "Si se cambia la imagen, primero súbela a POST /api/v1/images/upload " +
                          "y envía la URL resultante en imageUrl. La imagen anterior se elimina automáticamente del bucket."
    )
    public ResponseEntity<FlowerResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody FlowerRequest request) {
        return ResponseEntity.ok(flowerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Eliminar flor (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flowerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
