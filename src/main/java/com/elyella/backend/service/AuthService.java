package com.elyella.backend.service;

import com.elyella.backend.dto.request.LoginRequest;
import com.elyella.backend.dto.request.RegisterRequest;
import com.elyella.backend.dto.response.AuthResponse;
import com.elyella.backend.exception.DuplicateResourceException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.User;
import com.elyella.backend.model.enums.Role;
import com.elyella.backend.repository.UserRepository;
import com.elyella.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación: registro e inicio de sesión con JWT.
 * El token se devuelve en AuthResult para que el controller lo escriba como cookie HTTP-only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Transporta el JWT generado junto con la información pública del usuario.
     * Uso exclusivo interno entre AuthService y AuthController.
     */
    public record AuthResult(String jwtToken, AuthResponse userInfo) {}

    /**
     * Registra un nuevo usuario con rol USER.
     * Lanza DuplicateResourceException si el correo ya está en uso.
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el correo: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .address(request.address())
                .phone(request.phone())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.debug("Nuevo usuario registrado: {}", user.getEmail());

        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResult(token, new AuthResponse(user.getName(), user.getEmail(), user.getRole().name()));
    }

    /**
     * Retorna los datos públicos del usuario autenticado (usado por GET /auth/me).
     */
    @Transactional(readOnly = true)
    public AuthResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name());
    }

    /**
     * Autentica al usuario con email y contraseña.
     * Spring Security lanza BadCredentialsException si las credenciales son incorrectas.
     */
    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        log.debug("Login exitoso para: {}", request.email());
        return new AuthResult(token, new AuthResponse(user.getName(), user.getEmail(), user.getRole().name()));
    }
}
