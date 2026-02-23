package com.smartcommerce.dtos.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.smartcommerce.model.Product;

/**
 * GraphQL DTO for paginated Product responses
 * Maps Spring Data Page to GraphQL ProductPage type
 */
public record ProductPageGraphQL(
        List<Product> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
    /**
     * Creates a ProductPageGraphQL from a Spring Data Page object
     *
     * @param page Spring Data Page of Products
     * @return ProductPageGraphQL
     */
    public static ProductPageGraphQL of(Page<Product> page) {
        return new ProductPageGraphQL(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
