package com.smartcommerce.dtos.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Request body for creating a new order")
public record CreateOrderDTO(

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        @Schema(description = "List of items in the order", requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrderItemDTO> items
) {
}
