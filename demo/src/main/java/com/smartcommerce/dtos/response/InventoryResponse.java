package com.smartcommerce.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;

@Schema(description = "Response body for inventory information")
public record InventoryResponse(
        @Schema(description = "Inventory ID", example = "1")
        Integer inventoryId,

        @Schema(description = "Product ID", example = "1")
        Integer productId,

        String productName, @Schema(description = "Available quantity", example = "50")
        Integer quantityAvailable,

        @Schema(description = "Last updated timestamp")
        Timestamp lastUpdated
) {
}
