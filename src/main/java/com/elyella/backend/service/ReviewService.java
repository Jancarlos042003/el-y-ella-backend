package com.elyella.backend.service;

import com.elyella.backend.dto.request.ReviewRequest;
import com.elyella.backend.dto.response.ReviewResponse;
import com.elyella.backend.exception.DuplicateResourceException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Flower;
import com.elyella.backend.model.Review;
import com.elyella.backend.model.User;
import com.elyella.backend.repository.FlowerRepository;
import com.elyella.backend.repository.ReviewRepository;
import com.elyella.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de reseñas de flores.
 * Un usuario solo puede reseñar la misma flor una vez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final FlowerRepository flowerRepository;

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getFlower().getId(),
                review.getFlower().getName(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getComment(),
                review.getRating(),
                review.getCreatedAt()
        );
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    /** Retorna todas las reseñas de una flor ordenadas por fecha descendente. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> findByFlower(Long flowerId) {
        if (!flowerRepository.existsById(flowerId)) {
            throw new ResourceNotFoundException("Flor", flowerId);
        }
        return reviewRepository.findByFlowerIdOrderByCreatedAtDesc(flowerId).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Crea una reseña para una flor.
     * Lanza DuplicateResourceException si el usuario ya reseñó esa flor.
     */
    @Transactional
    public ReviewResponse create(String email, ReviewRequest request) {
        User user = loadUser(email);
        Flower flower = flowerRepository.findById(request.flowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Flor", request.flowerId()));

        if (reviewRepository.existsByFlowerIdAndUserId(flower.getId(), user.getId())) {
            throw new DuplicateResourceException("Ya has dejado una reseña para esta flor.");
        }

        Review review = Review.builder()
                .flower(flower)
                .user(user)
                .comment(request.comment())
                .rating(request.rating())
                .build();

        log.debug("Reseña creada por {} para flor id={}", email, flower.getId());
        return toResponse(reviewRepository.save(review));
    }

    /** Edita el comentario y calificación de una reseña propia. */
    @Transactional
    public ReviewResponse update(Long id, String email, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", id));

        if (!review.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Reseña", id);
        }

        review.setComment(request.comment());
        review.setRating(request.rating());
        return toResponse(review);
    }

    /**
     * Elimina una reseña.
     * El propietario puede eliminar la suya; un ADMIN puede eliminar cualquiera.
     */
    @Transactional
    public void delete(Long id, String email, boolean isAdmin) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", id));

        if (!isAdmin && !review.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Reseña", id);
        }

        reviewRepository.delete(review);
        log.debug("Reseña eliminada: id={}", id);
    }
}
