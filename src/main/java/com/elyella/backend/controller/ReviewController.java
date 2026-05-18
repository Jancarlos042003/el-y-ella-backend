package com.elyella.backend.controller;

import com.elyella.backend.dto.request.ReviewRequest;
import com.elyella.backend.dto.response.ReviewResponse;
import com.elyella.backend.service.ReviewService;
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
 * Endpoints de reseñas de flores.
 * Lectura pública; escritura requiere autenticación.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reseñas", description = "Reseñas de arreglos florales")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/flower/{flowerId}")
    @Operation(summary = "Listar reseñas de una flor")
    public ResponseEntity<List<ReviewResponse>> getByFlower(@PathVariable Long flowerId) {
        return ResponseEntity.ok(reviewService.findByFlower(flowerId));
    }

    @PostMapping
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Crear reseña", description = "Un usuario puede reseñar una flor solo una vez.")
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Editar reseña propia")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(id, userDetails.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Eliminar reseña", description = "El propietario puede eliminar la suya; un ADMIN puede eliminar cualquiera.")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.delete(id, userDetails.getUsername(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
