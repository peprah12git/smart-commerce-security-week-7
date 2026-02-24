package com.smartcommerce.dtos.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new product")
public record CreateProductDTO(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        @Schema(description = "Name of the product", example = "Wireless Bluetooth Headphones", requiredMode = Schema.RequiredMode.REQUIRED)
        String productName,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Product description", example = "High-quality wireless headphones with noise cancellation")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Schema(description = "Product price", example = "49.99", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be a positive number")
        @Schema(description = "ID of the category this product belongs to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer categoryId
) {
}