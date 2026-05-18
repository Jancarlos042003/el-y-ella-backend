package com.elyella.backend.repository;

import com.elyella.backend.model.Category;
import com.elyella.backend.model.Flower;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Especificaciones JPA para búsqueda dinámica de flores.
 * Se combinan con Specification.where().and() según los parámetros recibidos.
 */
public final class FlowerSpecification {

    private FlowerSpecification() {}

    /**
     * Filtra flores cuyo nombre contenga el texto dado (insensible a mayúsculas).
     */
    public static Specification<Flower> nameContains(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    /**
     * Filtra flores que pertenezcan a la categoría indicada.
     * Usa subquery EXISTS para evitar duplicados en la paginación con @ManyToMany.
     */
    public static Specification<Flower> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            var subRoot = sub.correlate(root);
            Join<Flower, Category> join = subRoot.join("categories");
            sub.select(cb.literal(1))
               .where(cb.equal(join.get("id"), categoryId));
            return cb.exists(sub);
        };
    }
}
