package com.elyella.backend.repository;

import com.elyella.backend.model.CheckoutAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutAttemptRepository extends JpaRepository<CheckoutAttempt, Long> {
    Optional<CheckoutAttempt> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
