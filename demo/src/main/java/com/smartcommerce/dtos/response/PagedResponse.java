package com.smartcommerce.dtos.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic DTO for paginated API responses
 * Wraps a list of items with pagination metadata
 *
 * @param <T> Type of items in the response
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
    /**
     * Creates a PagedResponse from a Spring Data Page object
     *
     * @param page Spring Data Page
     * @param <T>  Content type
     * @return PagedResponse
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
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
