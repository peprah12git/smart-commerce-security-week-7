package com.smartcommerce.dtos.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating an existing product")
public record UpdateProductDTO(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        @Schema(description = "Updated product name", example = "Wireless Bluetooth Headphones V2", requiredMode = Schema.RequiredMode.REQUIRED)
        String productName,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Updated product description", example = "Improved noise cancellation and longer battery life")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Schema(description = "Updated product price", example = "59.99", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be a positive number")
        @Schema(description = "Updated category ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer categoryId
) {
}