package com.smartcommerce.controller.restControllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.CreateProductDTO;
import com.smartcommerce.dtos.request.ProductFilterDTO;
import com.smartcommerce.dtos.request.UpdateProductDTO;
import com.smartcommerce.dtos.request.UpdateProductQuantityDTO;
import com.smartcommerce.dtos.response.PagedResponse;
import com.smartcommerce.dtos.response.ProductResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.Product;
import com.smartcommerce.security.RequiredRole;
import com.smartcommerce.service.serviceInterface.ProductService;
import com.smartcommerce.utils.ProductMapper;
import com.smartcommerce.validation.ValidSortDirection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Product management
 * Handles HTTP requests for product CRUD operations with pagination, sorting, and filtering
 * Base URL: /api/products
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management API — CRUD, search, pagination, and filtering")
public class ProductController {

    private final ProductService productService;

    // Manual constructor for compatibility
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Create a new product
     * POST /api/products
     */
    //Swagger /OpenAPI annotations
    //@Operation: gives human readeble description of what the api does
    @Operation(summary = "Create a new product", description = "Creates a new product with the provided details and optional initial stock quantity")
    // tells swagger what possible HTTP response the endpoint can return
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))

    })
    @PostMapping
    @RequiredRole("ADMIN") // Only admins can create products
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductDTO createProductDTO) {
// ---creating a new product entity from the incomming DTp
        Product product = new Product(
                createProductDTO.productName(),
                createProductDTO.description(),
                createProductDTO.price(),
                createProductDTO.categoryId()
        );

        if (createProductDTO.quantityAvailable() != null) {
            product.setQuantityAvailable(createProductDTO.quantityAvailable());
        }
//----- Map the saved Product entity to a response DTO This ensures we only send the necessary fields back to the client
        Product createdProduct = productService.createProduct(product);
        ProductResponse response = ProductMapper.toProductResponse(createdProduct);
//-----return HTTP with product data
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get all products (without pagination)
     * GET /api/products/all
     */
    @Operation(summary = "Get all products", description = "Retrieves all products without pagination")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/all")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductResponse> response = ProductMapper.toProductResponseList(products);
        return ResponseEntity.ok(response);
    }

    /**
     * Get products with pagination, sorting, and filtering
     * GET /api/products?page=0&size=10&sortBy=price&sortDirection=ASC&category=Electronics&minPrice=100&maxPrice=1000&searchTerm=phone&inStock=true
     *
     * @param page          Page number (default: 0)
     * @param size          Page size (default: 10, max: 100)
     * @param sortBy        Sort field (default: productId)
     *                      Options: productName, price, categoryName, quantity, createdAt, productId
     * @param sortDirection Sort direction (default: ASC)
     *                      Options: ASC, DESC
     * @param category      Filter by category name
     * @param minPrice      Filter by minimum price
     * @param maxPrice      Filter by maximum price
     * @param searchTerm    Search in product name and description
     * @param inStock       Filter by stock status (true=in stock, false=out of stock)
     */
    @Operation(summary = "Get products with pagination and filtering",
            description = "Retrieves products with support for pagination, sorting, and multiple filter criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated product list retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> getProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field", example = "productId",
                    schema = @Schema(allowableValues = {"productName", "price", "categoryName", "quantity", "createdAt", "productId"}))
            @RequestParam(defaultValue = "productId") String sortBy,
            @Parameter(description = "Sort direction", example = "ASC")
            @RequestParam(defaultValue = "ASC") @ValidSortDirection String sortDirection,
            @Parameter(description = "Filter by category name") @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Search in product name and description") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Filter by stock status") @RequestParam(required = false) Boolean inStock) {

        // Create filter DTO
        ProductFilterDTO filters = new ProductFilterDTO(
                category,
                minPrice,
                maxPrice,
                searchTerm,
                inStock
        );

        // Get paginated and filtered products
        List<Product> products = productService.getProductsWithPaginationAndFilters(
                page, size, sortBy, sortDirection, filters
        );

        // Get total count for pagination
        long totalElements = productService.countProductsWithFilters(filters);

        // Convert to response DTOs
        List<ProductResponse> productResponses = ProductMapper.toProductResponseList(products);

        // Create paged response
        PagedResponse<ProductResponse> response = new PagedResponse<>(
                productResponses,
                page,
                size,
                totalElements,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @Operation(summary = "Get product by ID", description = "Retrieves a single product by its unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int id) {
        Product product = productService.getProductById(id);
        ProductResponse response = ProductMapper.toProductResponse(product);
        return ResponseEntity.ok(response);
    }

    /**
     * Get products by category (without pagination)
     * GET /api/products/category/{categoryName}
     */
    @Operation(summary = "Get products by category", description = "Retrieves all products belonging to a specific category")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Category name", required = true, example = "Electronics")
            @PathVariable String categoryName) {
        List<Product> products = productService.getProductsByCategory(categoryName);
        List<ProductResponse> response = ProductMapper.toProductResponseList(products);
        return ResponseEntity.ok(response);
    }

    /**
     * Search products by name or description (without pagination)
     * GET /api/products/search?term={searchTerm}
     */
    @Operation(summary = "Search products", description = "Searches for products by name or description keyword")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Search term to match against product name/description", required = true, example = "headphones")
            @RequestParam String term) {
        List<Product> products = productService.searchProducts(term);
        List<ProductResponse> response = ProductMapper.toProductResponseList(products);
        return ResponseEntity.ok(response);
    }

    /**
     * Update product
     * PUT /api/products/{id}
     */
    @Operation(summary = "Update a product", description = "Fully updates an existing product with new details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product or category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RequiredRole("ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID to update", required = true, example = "1")
            @PathVariable int id,
            @Valid @RequestBody UpdateProductDTO updateProductDTO) {

        Product product = new Product();
        product.setProductName(updateProductDTO.productName());
        product.setDescription(updateProductDTO.description());
        product.setPrice(updateProductDTO.price());
        product.setCategoryId(updateProductDTO.categoryId());

        if (updateProductDTO.quantityAvailable() != null) {
            product.setQuantityAvailable(updateProductDTO.quantityAvailable());
        }

        Product updatedProduct = productService.updateProduct(id, product);
        ProductResponse response = ProductMapper.toProductResponse(updatedProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * Update product quantity only
     * PATCH /api/products/{id}/quantity
     */
    @Operation(summary = "Update product quantity", description = "Updates only the stock quantity of a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RequiredRole("ADMIN")
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<ProductResponse> updateProductQuantity(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable int id,
            @Valid @RequestBody UpdateProductQuantityDTO updateQuantityDTO) {

        Product updatedProduct = productService.updateProductQuantity(
                id,
                updateQuantityDTO.quantity()
        );
        ProductResponse response = ProductMapper.toProductResponse(updatedProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @Operation(summary = "Delete a product", description = "Permanently deletes a product by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RequiredRole("ADMIN")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID to delete", required = true, example = "1")
            @PathVariable int id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear product cache
     * DELETE /api/products/cache
     */
    @Operation(summary = "Clear product cache", description = "Clears the product cache to force fresh data retrieval")
    @ApiResponse(responseCode = "204", description = "Cache cleared successfully")
    @RequiredRole("ADMIN")
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearProductCache() {
        productService.invalidateProductCache();
        return ResponseEntity.noContent().build();
    }
}