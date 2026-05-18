package com.elyella.backend.controller;

import com.elyella.backend.dto.request.LoginRequest;
import com.elyella.backend.dto.request.RegisterRequest;
import com.elyella.backend.dto.response.AuthResponse;
import com.elyella.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación.
 * El JWT se entrega como cookie HTTP-only (no en el body) para proteger contra XSS.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y cierre de sesión")
public class AuthController {

    private final AuthService authService;

    @Value("${cookie.name}")
    private String cookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @Value("${cookie.max-age-seconds}")
    private long cookieMaxAge;

    @PostMapping("/register")
    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una cuenta con rol USER. El JWT se entrega en la cookie 'jwt' (HTTP-only)."
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        addJwtCookie(response, result.jwtToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.userInfo());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica con email y contraseña. El JWT se entrega en la cookie 'jwt' (HTTP-only)."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        addJwtCookie(response, result.jwtToken());
        return ResponseEntity.ok(result.userInfo());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida la sesión borrando la cookie JWT del navegador."
    )
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Obtener usuario autenticado",
            description = "Retorna los datos del usuario cuyo JWT está en la cookie. " +
                    "Permite al frontend restaurar el estado de sesión al recargar la página."
    )
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.me(userDetails.getUsername()));
    }

    /**
     * Escribe la cookie JWT con los flags de seguridad configurados.
     * Usa ResponseCookie (Spring Web) para poder incluir el atributo SameSite,
     * que javax.servlet.http.Cookie no soporta.
     */
    private void addJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(cookieMaxAge)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Sobreescribe la cookie con valor vacío y maxAge=0 para que el browser la elimine. */
    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
