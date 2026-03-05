package com.smartcommerce.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request body for submitting a product review")
public record CreateReviewDTO(

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be a positive number")
        @Schema(description = "ID of the product being reviewed", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer productId,

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        @Schema(description = "Product rating (1–5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        int rating,

        @NotBlank(message = "Comment is required")
        @Schema(description = "Review comment", example = "Great product, fast delivery!", requiredMode = Schema.RequiredMode.REQUIRED)
        String comment
) {
}
