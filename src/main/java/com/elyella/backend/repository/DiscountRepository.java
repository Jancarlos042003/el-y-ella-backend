package com.elyella.backend.repository;

import com.elyella.backend.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByFlowerId(Long flowerId);

    @Query("""
            SELECT d FROM Discount d
            WHERE d.flower.id = :flowerId
              AND d.active = true
              AND d.startDate <= :today
              AND (d.endDate IS NULL OR d.endDate >= :today)
            """)
    Optional<Discount> findActiveByFlowerId(@Param("flowerId") Long flowerId,
                                             @Param("today") LocalDate today);

    @Query("""
            SELECT d FROM Discount d JOIN FETCH d.flower
            WHERE d.active = true
              AND d.startDate <= :today
              AND (d.endDate IS NULL OR d.endDate >= :today)
            """)
    List<Discount> findAllActive(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(d) > 0 FROM Discount d
            WHERE d.flower.id = :flowerId
              AND d.active = true
              AND d.id <> :excludeId
              AND d.startDate <= :endDate
              AND (d.endDate IS NULL OR d.endDate >= :startDate)
            """)
    boolean existsOverlappingDiscount(@Param("flowerId") Long flowerId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("excludeId") Long excludeId);
}
