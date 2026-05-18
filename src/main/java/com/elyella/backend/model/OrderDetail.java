package com.elyella.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa un ítem dentro de un pedido.
 * Migrada de la tabla 'detalle_pedido' del legado.
 * El precio se almacena como snapshot al momento de la compra.
 */
@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flower_id", nullable = false)
    private Flower flower;

    @Column(nullable = false)
    private int quantity;

    /**
     * Precio unitario en el momento de la compra (snapshot).
     * No referencia Flower.price para preservar histórico.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
