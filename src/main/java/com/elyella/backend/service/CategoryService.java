package com.elyella.backend.service;

import com.elyella.backend.dto.request.CategoryRequest;
import com.elyella.backend.dto.response.CategoryResponse;
import com.elyella.backend.exception.DuplicateResourceException;
import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.Category;
import com.elyella.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio CRUD para la gestión de categorías de flores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }

    /** Retorna todas las categorías disponibles. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Retorna una categoría por su ID. */
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id)));
    }

    /** Crea una nueva categoría. Lanza DuplicateResourceException si el nombre ya existe. */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Ya existe una categoría con el nombre: " + request.name());
        }
        Category saved = categoryRepository.save(Category.builder().name(request.name()).build());
        log.debug("Categoría creada: id={}, nombre={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    /** Actualiza el nombre de una categoría existente. */
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
        category.setName(request.name());
        return toResponse(category);
    }

    /** Elimina una categoría. */
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoría", id);
        }
        categoryRepository.deleteById(id);
        log.debug("Categoría eliminada: id={}", id);
    }
}
