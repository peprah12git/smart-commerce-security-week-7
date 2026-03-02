package com.smartcommerce.dtos.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for creating a new order")
public record CreateOrderDTO(

        @NotNull(message = "User ID is required")
        @Schema(description = "ID of the user placing the order", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer userId,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        @Schema(description = "List of items in the order", requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrderItemDTO> items
) {
}
