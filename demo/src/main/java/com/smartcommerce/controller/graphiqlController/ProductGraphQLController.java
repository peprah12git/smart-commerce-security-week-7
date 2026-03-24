package com.smartcommerce.controller.graphiqlController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.smartcommerce.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
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
            @Argument int categoryId) {

        Product product = new Product();
        product.setName(productName);
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(price));

        return productService.createProduct(product, categoryId);
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
            existingProduct.setName(productName);
        }
        if (description != null) {
            existingProduct.setDescription(description);
        }
        if (price != null) {
            existingProduct.setPrice(BigDecimal.valueOf(price));
        }
        if (categoryId != null) {
            return productService.updateProduct(id, existingProduct, categoryId);
        }
        
        return productService.updateProduct(id, existingProduct, null);
    }

    // ==================== FIELD RESOLVERS ====================

    /**
     * Resolve productName field for Product type
     * Maps Product.name to GraphQL Product.productName
     */
    @SchemaMapping(typeName = "Product", field = "productName")
    public String productName(Product product) {
        return product.getName();
    }

    /**
     * Resolve categoryId field for Product type
     * Extracts category ID from Product.category
     */
    @BatchMapping(typeName = "Product", field = "categoryId")
    public Map<Product, Integer> categoryId(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> p.getCategory() != null ? p.getCategory().getCategoryId() : null
                ));
    }

    @BatchMapping(typeName = "Product", field = "categoryName")
    public Map<Product, String> categoryName(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> p.getCategory() != null ? p.getCategory().getCategoryName() : null
                ));
    }
    @BatchMapping(typeName = "Product", field = "inventory")
    public Map<Product, Inventory> inventory(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> p.getInventory()
                ));
    }

}
