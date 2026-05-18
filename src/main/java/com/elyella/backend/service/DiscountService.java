package com.elyella.backend.service;

import com.elyella.backend.dto.request.DiscountRequest;
import com.elyella.backend.dto.response.DiscountResponse;
import com.elyella.backend.exception.DuplicateResourceException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Discount;
import com.elyella.backend.model.Flower;
import com.elyella.backend.repository.DiscountRepository;
import com.elyella.backend.repository.FlowerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final FlowerRepository flowerRepository;

    private DiscountResponse toResponse(Discount d) {
        return new DiscountResponse(
                d.getId(),
                d.getFlower().getId(),
                d.getFlower().getName(),
                d.getDiscountPercentage(),
                d.getStartDate(),
                d.getEndDate(),
                d.getCreatedAt(),
                d.isActive()
        );
    }

    private void checkNoOverlap(Long flowerId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        LocalDate effectiveEnd = (endDate != null) ? endDate : LocalDate.MAX;
        if (discountRepository.existsOverlappingDiscount(flowerId, startDate, effectiveEnd, excludeId)) {
            throw new DuplicateResourceException(
                    "Ya existe un descuento activo para esta flor que se solapa con las fechas indicadas.");
        }
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> findAll() {
        return discountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DiscountResponse findById(Long id) {
        return toResponse(discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento", id)));
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> findByFlower(Long flowerId) {
        if (!flowerRepository.existsById(flowerId)) {
            throw new ResourceNotFoundException("Flor", flowerId);
        }
        return discountRepository.findByFlowerId(flowerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> findActiveDiscounts() {
        return discountRepository.findAllActive(LocalDate.now()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<DiscountResponse> findActiveByFlower(Long flowerId) {
        return discountRepository.findActiveByFlowerId(flowerId, LocalDate.now()).map(this::toResponse);
    }

    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        Flower flower = flowerRepository.findById(request.flowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Flor", request.flowerId()));

        checkNoOverlap(request.flowerId(), request.startDate(), request.endDate(), -1L);

        Discount discount = Discount.builder()
                .flower(flower)
                .discountPercentage(request.discountPercentage())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        Discount saved = discountRepository.save(discount);
        log.debug("Descuento creado: id={}, flor={}, porcentaje={}",
                saved.getId(), flower.getName(), saved.getDiscountPercentage());
        return toResponse(saved);
    }

    @Transactional
    public DiscountResponse update(Long id, DiscountRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento", id));

        Flower flower = flowerRepository.findById(request.flowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Flor", request.flowerId()));

        checkNoOverlap(request.flowerId(), request.startDate(), request.endDate(), id);

        discount.setFlower(flower);
        discount.setDiscountPercentage(request.discountPercentage());
        discount.setStartDate(request.startDate());
        discount.setEndDate(request.endDate());
        log.debug("Descuento actualizado: id={}", id);
        return toResponse(discount);
    }

    @Transactional
    public DiscountResponse deactivate(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento", id));
        discount.setActive(false);
        log.debug("Descuento desactivado: id={}", id);
        return toResponse(discount);
    }

    @Transactional
    public void delete(Long id) {
        if (!discountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Descuento", id);
        }
        discountRepository.deleteById(id);
        log.debug("Descuento eliminado: id={}", id);
    }
}
