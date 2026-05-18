package com.elyella.backend.service;

import com.elyella.backend.dto.request.FlowerRequest;
import com.elyella.backend.dto.response.CategoryResponse;
import com.elyella.backend.dto.response.FlowerResponse;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Category;
import com.elyella.backend.model.Flower;
import com.elyella.backend.repository.CategoryRepository;
import com.elyella.backend.repository.FlowerRepository;
import com.elyella.backend.repository.FlowerSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio CRUD para la gestión del catálogo de flores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowerService {

    private final FlowerRepository flowerRepository;
    private final CategoryRepository categoryRepository;
    private final GcsService gcsService;

    private FlowerResponse toResponse(Flower flower) {
        Set<CategoryResponse> categories = flower.getCategories().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .collect(Collectors.toSet());
        return new FlowerResponse(
                flower.getId(), flower.getName(), flower.getDescription(),
                flower.getPrice(), flower.getStock(), flower.getImageUrl(),
                categories, flower.getCreatedAt()
        );
    }

    /**
     * Resuelve y valida que todos los IDs de categorías existen en la BD.
     * Lanza ResourceNotFoundException si alguna categoría no existe.
     */
    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        Set<Category> found = new HashSet<>(categoryRepository.findAllById(categoryIds));
        if (found.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("Una o más categorías indicadas no existen.");
        }
        return found;
    }

    /** Retorna todas las flores del catálogo sin filtros (compatibilidad interna). */
    @Transactional(readOnly = true)
    public List<FlowerResponse> findAll() {
        return flowerRepository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Búsqueda paginada del catálogo con filtros opcionales.
     *
     * @param q          texto libre para buscar por nombre (ILIKE)
     * @param categoryId filtro por categoría
     * @param sort       criterio de orden: newest, price_asc, price_desc, bestseller
     * @param page       página (0-indexed)
     * @param size       ítems por página
     */
    @Transactional(readOnly = true)
    public Page<FlowerResponse> search(String q, Long categoryId, String sort, int page, int size) {
        Specification<Flower> spec = Specification.allOf();

        if (q != null && !q.isBlank()) {
            spec = spec.and(FlowerSpecification.nameContains(q.trim()));
        }
        if (categoryId != null) {
            spec = spec.and(FlowerSpecification.hasCategory(categoryId));
        }

        Sort sorting = switch (sort) {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            // bestseller: pendiente de subquery por volumen de ventas — usa newest como MVP
            default           -> Sort.by("createdAt").descending();
        };

        return flowerRepository.findAll(spec, PageRequest.of(page, size, sorting))
                .map(this::toResponse);
    }

    /** Retorna una flor por su ID. */
    @Transactional(readOnly = true)
    public FlowerResponse findById(Long id) {
        return toResponse(flowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flor", id)));
    }

    /** Retorna las flores que pertenecen a una categoría. */
    @Transactional(readOnly = true)
    public List<FlowerResponse> findByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Categoría", categoryId);
        }
        return flowerRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    /** Crea una nueva flor con sus categorías asociadas. */
    @Transactional
    public FlowerResponse create(FlowerRequest request) {
        Flower flower = Flower.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .categories(resolveCategories(request.categoryIds()))
                .build();
        Flower saved = flowerRepository.save(flower);
        log.debug("Flor creada: id={}, nombre={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    /** Actualiza los datos de una flor existente. Elimina la imagen anterior de GCS si cambió. */
    @Transactional
    public FlowerResponse update(Long id, FlowerRequest request) {
        Flower flower = flowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flor", id));

        String oldImageUrl = flower.getImageUrl();
        if (oldImageUrl != null && !Objects.equals(oldImageUrl, request.imageUrl())) {
            gcsService.deleteImage(oldImageUrl);
        }

        flower.setName(request.name());
        flower.setDescription(request.description());
        flower.setPrice(request.price());
        flower.setStock(request.stock());
        flower.setImageUrl(request.imageUrl());
        flower.setCategories(resolveCategories(request.categoryIds()));
        log.debug("Flor actualizada: id={}", id);
        return toResponse(flower);
    }

    /** Elimina una flor del catálogo y su imagen asociada de GCS. */
    @Transactional
    public void delete(Long id) {
        Flower flower = flowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flor", id));

        if (flower.getImageUrl() != null) {
            gcsService.deleteImage(flower.getImageUrl());
        }

        flowerRepository.delete(flower);
        log.debug("Flor eliminada: id={}", id);
    }
}
