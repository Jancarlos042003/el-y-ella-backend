package com.elyella.backend.model;

import com.elyella.backend.model.enums.CheckoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para registrar intentos de checkout y garantizar la idempotencia.
 */
@Entity
@Table(name = "checkout_attempts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "idempotency_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CheckoutAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckoutStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    public boolean isSuccess() {
        return CheckoutStatus.SUCCESS.equals(this.status);
    }
}
