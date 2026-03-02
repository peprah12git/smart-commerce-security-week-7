package com.smartcommerce.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request body for adding an item to cart")
public record AddToCartDTO(

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be a positive number")
        @Schema(description = "ID of the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer userId,

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be a positive number")
        @Schema(description = "ID of the product to add", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Schema(description = "Quantity to add", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer quantity
) {
}
