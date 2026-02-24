package com.smartcommerce.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.smartcommerce.dtos.response.ProductResponse;
import com.smartcommerce.model.Product;

/**
 * Mapper utility class for Product entity and DTOs
 */
public class ProductMapper {

    /**
     * Converts Product entity to ProductResponse DTO
     */
    public static ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse();
        response.setProductId(product.getProductId());
        response.setProductName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        
        // Set category details from relationship
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getCategoryId());
            response.setCategoryName(product.getCategory().getCategoryName());
        }
        
        response.setCreatedAt(product.getCreatedAt());

        return response;
    }

    /**
     * Converts list of Product entities to list of ProductResponse DTOs
     */
    public static List<ProductResponse> toProductResponseList(List<Product> products) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(ProductMapper::toProductResponse)
                .collect(Collectors.toList());
    }
}