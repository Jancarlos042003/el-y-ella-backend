package com.elyella.backend.controller;

import com.elyella.backend.dto.request.UpdateUserRequest;
import com.elyella.backend.dto.response.UserResponse;
import com.elyella.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de perfil del usuario autenticado.
 * Separado de AdminController para que el usuario no pueda especificar un ID arbitrario.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Perfil", description = "Gestión del perfil del usuario autenticado")
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza nombre, dirección y/o teléfono. Email y rol no son modificables."
    )
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMe(userDetails.getUsername(), request));
    }
}
