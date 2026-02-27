package com.smartcommerce.controller.restControllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.UpdateProductQuantityDTO;
import com.smartcommerce.dtos.response.InventoryResponse;
import com.smartcommerce.dtos.response.PagedResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.Inventory;
import com.smartcommerce.security.RequiredRole;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Inventory management
 * Handles HTTP requests for inventory operations
 * Base URL: /api/inventory
 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management API")
public class InventoryController {

    private final InventoryServiceInterface inventoryService;

    public InventoryController(InventoryServiceInterface inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Get all inventory items with pagination
     * GET /api/inventory/paged?page=0&size=10&sort=quantityAvailable,asc
     */
    @Operation(summary = "Get all inventory (paginated)", 
               description = "Retrieves all inventory items with pagination. Default page size is 10, sorted by quantity available ascending.")
    @ApiResponse(responseCode = "200", description = "Paginated inventory items retrieved successfully")
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<InventoryResponse>> getAllInventoryPaged(
            @PageableDefault(size = 10, sort = "quantityAvailable", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<Inventory> inventoryPage = inventoryService.getAllInventory(pageable);
        Page<InventoryResponse> responsePage = inventoryPage.map(this::toInventoryResponse);
        PagedResponse<InventoryResponse> pagedResponse = PagedResponse.of(responsePage);

        return ResponseEntity.ok(pagedResponse);
    }

    /**
     * Get inventory for a specific product
     * GET /api/inventory/{productId}
     */
    @Operation(summary = "Get inventory by product ID", description = "Retrieves inventory for a specific product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory found"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int productId) {
        Inventory inventory = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(toInventoryResponse(inventory));
    }

    /**
     * Get low stock items
     * GET /api/inventory/low-stock?threshold=10
     */
    @Operation(summary = "Get low stock items", description = "Retrieves products with stock below threshold")
    @ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully")
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems(
            @Parameter(description = "Stock threshold", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {
        List<Inventory> items = inventoryService.getLowStockItems(threshold);
        List<InventoryResponse> responses = items.stream()
                .map(this::toInventoryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get low stock items with pagination
     * GET /api/inventory/low-stock/paged?threshold=10&page=0&size=10&sort=quantityAvailable,asc
     */
    @Operation(summary = "Get low stock items (paginated)", 
               description = "Retrieves products with stock below threshold with pagination. Default page size is 10.")
    @ApiResponse(responseCode = "200", description = "Paginated low stock items retrieved successfully")
    @GetMapping("/low-stock/paged")
    public ResponseEntity<PagedResponse<InventoryResponse>> getLowStockItemsPaged(
            @Parameter(description = "Stock threshold", example = "10")
            @RequestParam(defaultValue = "10") int threshold,
            @PageableDefault(size = 10, sort = "quantityAvailable", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<Inventory> inventoryPage = inventoryService.getLowStockItems(threshold, pageable);
        Page<InventoryResponse> responsePage = inventoryPage.map(this::toInventoryResponse);
        PagedResponse<InventoryResponse> pagedResponse = PagedResponse.of(responsePage);

        return ResponseEntity.ok(pagedResponse);
    }

    /**
     * Get out of stock items
     * GET /api/inventory/out-of-stock
     */
    @Operation(summary = "Get out of stock items", description = "Retrieves all products with zero stock")
    @ApiResponse(responseCode = "200", description = "Out of stock items retrieved successfully")
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockItems() {
        List<Inventory> items = inventoryService.getOutOfStockItems();
        List<InventoryResponse> responses = items.stream()
                .map(this::toInventoryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Check if product is in stock
     * GET /api/inventory/{productId}/check
     */
    @Operation(summary = "Check if product is in stock", description = "Checks if a product has available stock")
    @ApiResponse(responseCode = "200", description = "Stock status retrieved")
    @GetMapping("/{productId}/check")
    public ResponseEntity<Boolean> checkStock(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int productId) {
        boolean inStock = inventoryService.isInStock(productId);
        return ResponseEntity.ok(inStock);
    }

    /**
     * Update inventory quantity
     * PUT /api/inventory/{productId}
     */
    @Operation(summary = "Update inventory quantity", description = "Sets the stock quantity to a new value")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    //PUT "/api/inventory/1?quantity=40"
    @PutMapping("/{productId}")
    @RequiredRole("ADMIN")
    public ResponseEntity<Void> updateInventory(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int productId,
            @Valid @RequestBody UpdateProductQuantityDTO quantityDTO) {
        inventoryService.updateInventory(productId, quantityDTO.quantity());
        return ResponseEntity.ok().build();
    }

    /**
     * Add stock to a product
     * POST /api/inventory/{productId}/stock-additions
     */
    @Operation(summary = "Add stock to product", description = "Increases the stock quantity by the specified amount")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock added successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{productId}/stock-additions")
    @RequiredRole("ADMIN")
    public ResponseEntity<Void> addStock(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int productId,
            @Valid @RequestBody UpdateProductQuantityDTO quantityDTO) {
        inventoryService.addStock(productId, quantityDTO.quantity());
        return ResponseEntity.ok().build();
    }

    /**
     * Reduce stock from a product
     * POST /api/inventory/{productId}/stock-reductions
     */
    @Operation(summary = "Reduce stock from product", description = "Decreases the stock quantity by the specified amount")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reduced successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{productId}/stock-reductions")
    @RequiredRole("ADMIN")
    public ResponseEntity<Void> reduceStock(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int productId,
            @Valid @RequestBody UpdateProductQuantityDTO quantityDTO) {
        inventoryService.reduceStock(productId, quantityDTO.quantity());
        return ResponseEntity.ok().build();
    }

    private InventoryResponse toInventoryResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getInventoryId(),
                inventory.getProduct().getProductId(),
                inventory.getProductName(),
                inventory.getQuantityAvailable(),
                inventory.getLastUpdated()
        );
    }
}
