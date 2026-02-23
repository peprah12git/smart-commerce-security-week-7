package com.smartcommerce.controller.graphiqlController;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcommerce.dtos.request.ProductFilterDTO;
import com.smartcommerce.dtos.response.ProductPageGraphQL;
import com.smartcommerce.model.Product;
import com.smartcommerce.service.serviceInterface.ProductService;

/**
 * GraphQL Controller for Product operations
 * Handles GraphQL queries and mutations for products
 * Coexists with REST endpoints in ProductController
 */
@Controller
public class ProductGraphQLController {

    private final ProductService productService;

    public ProductGraphQLController(ProductService productService) {
        this.productService = productService;
    }

    // ==================== QUERIES ====================

    /**
     * Get a single product by ID
     * GraphQL Query: product(id: Int!): Product
     */
    @QueryMapping
    public Product product(@Argument int id) {
        return productService.getProductById(id);
    }

    /**
     * Get all products with optional filters
     * GraphQL Query: products(...): [Product!]!
     */
    @QueryMapping
    public List<Product> products(
            @Argument String category,
            @Argument Double minPrice,
            @Argument Double maxPrice,
            @Argument String searchTerm) {

        // If any filters provided, use filtered query
        if (category != null || minPrice != null || maxPrice != null || searchTerm != null) {
            ProductFilterDTO filters = new ProductFilterDTO(
                    category,
                    minPrice != null ? BigDecimal.valueOf(minPrice) : null,
                    maxPrice != null ? BigDecimal.valueOf(maxPrice) : null,
                    searchTerm,
                    null  // inStock filter
            );

            return productService.getProductsWithFilters("productId", "ASC", filters);
        }

        // Return all products if no filters
        return productService.getAllProducts();
    }

    /**
     * Search products by search term
     * GraphQL Query: searchProducts(searchTerm: String!): [Product!]!
     */
    @QueryMapping
    public List<Product> searchProducts(@Argument String searchTerm) {
        return productService.searchProducts(searchTerm);
    }

    /**
     * Get paginated products with optional filters
     * GraphQL Query: productsPaged(...): ProductPage!
     * Default page size is 10
     */
    @QueryMapping
    public ProductPageGraphQL productsPaged(
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection,
            @Argument String category,
            @Argument Double minPrice,
            @Argument Double maxPrice,
            @Argument String searchTerm) {

        // Set defaults
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        String sortField = sortBy != null ? sortBy : "productId";
        String direction = sortDirection != null ? sortDirection : "ASC";

        // Create pageable
        Sort.Direction sortDir = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortDir, sortField));

        // Create filters
        ProductFilterDTO filters = new ProductFilterDTO(
                category,
                minPrice != null ? BigDecimal.valueOf(minPrice) : null,
                maxPrice != null ? BigDecimal.valueOf(maxPrice) : null,
                searchTerm,
                null  // inStock filter
        );

        // Get paginated results
        Page<Product> productsPage = productService.getProductsWithFilters(pageable, filters);
        return ProductPageGraphQL.of(productsPage);
    }

    /**
     * Search products with pagination
     * GraphQL Query: searchProductsPaged(...): ProductPage!
     * Default page size is 10
     */
    @QueryMapping
    public ProductPageGraphQL searchProductsPaged(
            @Argument String searchTerm,
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        // Set defaults
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        String sortField = sortBy != null ? sortBy : "productName";
        String direction = sortDirection != null ? sortDirection : "ASC";

        // Create pageable
        Sort.Direction sortDir = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortDir, sortField));

        // Get paginated search results
        Page<Product> productsPage = productService.searchProducts(searchTerm, pageable);
        return ProductPageGraphQL.of(productsPage);
    }

    // ==================== MUTATIONS ====================

    /**
     * Create a new product
     * GraphQL Mutation: createProduct(...): Product!
     */
    @MutationMapping
    public Product createProduct(
            @Argument String productName,
            @Argument String description,
            @Argument Double price,
            @Argument int categoryId,
            @Argument Integer quantityAvailable) {

        Product product = new Product(
                productName,
                description,
                BigDecimal.valueOf(price),
                categoryId
        );

        if (quantityAvailable != null) {
            product.setQuantityAvailable(quantityAvailable);
        }

        return productService.createProduct(product);
    }

    /**
     * Update an existing product
     * GraphQL Mutation: updateProduct(...): Product!
     */
    @MutationMapping
    public Product updateProduct(
            @Argument int id,
            @Argument String productName,
            @Argument String description,
            @Argument Double price,
            @Argument Integer categoryId) {

        Product existingProduct = productService.getProductById(id);

        if (productName != null) {
            existingProduct.setProductName(productName);
        }
        if (description != null) {
            existingProduct.setDescription(description);
        }
        if (price != null) {
            existingProduct.setPrice(BigDecimal.valueOf(price));
        }
        if (categoryId != null) {
            existingProduct.setCategoryId(categoryId);
        }

        return productService.updateProduct(id, existingProduct);
    }

    /**
     * Update product quantity
     * GraphQL Mutation: updateProductQuantity(id: Int!, quantity: Int!): Product!
     */
    @MutationMapping
    public Product updateProductQuantity(
            @Argument int id,
            @Argument int quantity) {
        return productService.updateProductQuantity(id, quantity);
    }

    /**
     * Delete a product
     * GraphQL Mutation: deleteProduct(id: Int!): Boolean!
     */
    @MutationMapping
    public boolean deleteProduct(@Argument int id) {
        productService.deleteProduct(id);
        return true;
    }
}
