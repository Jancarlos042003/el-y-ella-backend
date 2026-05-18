package com.elyella.backend.service;

import com.elyella.backend.dto.request.UpdateUserRequest;
import com.elyella.backend.dto.response.UserResponse;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.User;
import com.elyella.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la administración de usuarios (CRUD restringido a ADMIN).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getAddress(), user.getPhone(), user.getRole(), user.getCreatedAt()
        );
    }

    /** Retorna todos los usuarios registrados. */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Retorna un usuario por su ID. */
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id)));
    }

    /**
     * Permite al usuario autenticado actualizar su propio perfil.
     * Email y rol no son modificables desde este método.
     */
    @Transactional
    public UserResponse updateMe(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email: " + email));
        if (request.name() != null && !request.name().isBlank()) user.setName(request.name());
        if (request.address() != null) user.setAddress(request.address());
        if (request.phone() != null) user.setPhone(request.phone());
        log.debug("Perfil actualizado por el propio usuario: {}", email);
        return toResponse(user);
    }

    /** Actualiza nombre, dirección y/o teléfono de un usuario. Los campos nulos se ignoran. */
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (request.name() != null && !request.name().isBlank()) user.setName(request.name());
        if (request.address() != null) user.setAddress(request.address());
        if (request.phone() != null) user.setPhone(request.phone());

        log.debug("Usuario actualizado: id={}", id);
        return toResponse(user);
    }

    /** Elimina un usuario por su ID. */
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario", id);
        }
        userRepository.deleteById(id);
        log.debug("Usuario eliminado: id={}", id);
    }
}
