package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.dtos.request.ProductFilterDTO;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Category;
import com.smartcommerce.model.Product;
import com.smartcommerce.repositories.CategoryRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.service.serviceInterface.ProductService;
import com.smartcommerce.sorting.SortStrategy;

/**
 * Service implementation for Product entity
 * Handles business logic, validation, sorting, and filtering
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SortStrategy<Product> sortStrategy;

    // Manual constructor for dependency injection
    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              SortStrategy<Product> sortStrategy) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.sortStrategy = sortStrategy;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", key = "#result.productId")
    })
    public Product createProduct(Product product) {
        // Business validation
        validateProduct(product);
        // check if category exist
        Category category = categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", product.getCategoryId()));

        Product savedProduct = productRepository.save(product);
        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsWithFilters(
            String sortBy,
            String sortDirection,
            ProductFilterDTO filters) {

        List<Product> products = productRepository.findAll();

        if (filters != null && filters.hasFilters()) {
            products = applyFilters(products, filters);
        }

        return applySorting(products, sortBy, sortDirection);
    }

    /**
     * Apply filters to product list
     */
    private List<Product> applyFilters(List<Product> products, ProductFilterDTO filters) {
        return products.stream()
                .filter(p -> matchesCategory(p, filters.category()))
                .filter(p -> matchesPriceRange(p, filters.minPrice(), filters.maxPrice()))
                .filter(p -> matchesSearchTerm(p, filters.searchTerm()))
                .filter(p -> matchesStockStatus(p, filters.inStock()))
                .collect(Collectors.toList());
    }

    /**
     * Check if product matches category filter
     */
    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.trim().isEmpty()) {
            return true;
        }
        return product.getCategoryName() != null &&
                product.getCategoryName().equalsIgnoreCase(category.trim());
    }

    /**
     * Check if product matches price range filter
     */
    private boolean matchesPriceRange(Product product, BigDecimal minPrice, BigDecimal maxPrice) {
        if (product.getPrice() == null) {
            return false;
        }

        if (minPrice != null && product.getPrice().compareTo(minPrice) < 0) {
            return false;
        }

        if (maxPrice != null && product.getPrice().compareTo(maxPrice) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if product matches search term (name or description)
     */
    private boolean matchesSearchTerm(Product product, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        String term = searchTerm.toLowerCase().trim();
        boolean matchesName = product.getProductName() != null &&
                product.getProductName().toLowerCase().contains(term);
        boolean matchesDescription = product.getDescription() != null &&
                product.getDescription().toLowerCase().contains(term);

        return matchesName || matchesDescription;
    }

    /**
     * Check if product matches stock status filter
     */
    private boolean matchesStockStatus(Product product, Boolean inStock) {
        if (inStock == null) {
            return true;
        }

        boolean productInStock = product.getQuantityAvailable() > 0;
        return productInStock == inStock;
    }

    /**
     * Apply sorting to product list using the injected sort strategy (Merge Sort)
     */
    private List<Product> applySorting(List<Product> products, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "productId"; // Default sort
        }

        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "ASC"; // Default direction
        }

        Comparator<Product> comparator = getComparator(sortBy);

        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        // Use the injected sort strategy (Merge Sort) for sorting
        return sortStrategy.sort(products, comparator);
    }

    /**
     * Get comparator based on sort field
     */
    private Comparator<Product> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "productname", "name" -> Comparator.comparing(
                    p -> p.getProductName() != null ? p.getProductName().toLowerCase() : "",
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "price" -> Comparator.comparing(
                    Product::getPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "categoryname", "category" -> Comparator.comparing(
                    p -> p.getCategoryName() != null ? p.getCategoryName().toLowerCase() : "",
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "quantity", "quantityavailable" -> Comparator.comparingInt(Product::getQuantityAvailable);
            case "createdat" -> Comparator.comparing(
                    Product::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "productid", "id" -> Comparator.comparingInt(Product::getProductId);
            default -> throw new BusinessException("Invalid sort field: " + sortBy +
                    ". Valid fields: productName, price, categoryName, quantity, createdAt, productId");
        };
    }

@Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#productId")
    public Product getProductById(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'category:' + #categoryName")
    public List<Product> getProductsByCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new BusinessException("Category name cannot be empty");
        }

        return productRepository.findByCategoryName(categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'search:' + #searchTerm")
    public List<Product> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new BusinessException("Search term cannot be empty");
        }

        return productRepository.searchProducts(searchTerm);
    }

    @Override
    @Caching(
        put = @CachePut(value = "product", key = "#productId"),
        evict = @CacheEvict(value = "products", allEntries = true)
    )
    public Product updateProduct(int productId, Product productDetails) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        validateProduct(productDetails);

        if (existingProduct.getCategoryId() != productDetails.getCategoryId()) {
            categoryRepository.findById(productDetails.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDetails.getCategoryId()));
        }

        existingProduct.setProductName(productDetails.getProductName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setCategoryId(productDetails.getCategoryId());

        if (productDetails.getQuantityAvailable() >= 0) {
            existingProduct.setQuantityAvailable(productDetails.getQuantityAvailable());
        }

        return productRepository.save(existingProduct);
    }

    @Override
    public Product updateProductQuantity(int productId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException("Quantity cannot be negative");
        }

        Product product = getProductById(productId);
        product.setQuantityAvailable(quantity);

        return productRepository.save(product);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", key = "#productId")
    })
    public void deleteProduct(int productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        productRepository.deleteById(productId);
    }

    @Override
    public void invalidateProductCache() {
        // No cache with JPA/Spring Data - managed by JPA provider
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProductsWithFilters(Pageable pageable, ProductFilterDTO filters) {
        // If no filters, use repository pagination directly
        if (filters == null || !filters.hasFilters()) {
            return productRepository.findAll(pageable);
        }

        // With filters, we need to fetch all, filter, then create a page
        // Note: This is less efficient than database-level filtering
        // For production, consider using Specifications or QueryDSL
        List<Product> allProducts = productRepository.findAll();
        List<Product> filteredProducts = applyFilters(allProducts, filters);

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredProducts.size());
        List<Product> pageContent = filteredProducts.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredProducts.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String categoryName, Pageable pageable) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new BusinessException("Category name cannot be empty");
        }

        return productRepository.findByCategoryName(categoryName, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new BusinessException("Search term cannot be empty");
        }

        return productRepository.searchProducts(searchTerm, pageable);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new BusinessException("Product cannot be null");
        }

        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            throw new BusinessException("Product name is required");
        }

        if (product.getProductName().length() > 255) {
            throw new BusinessException("Product name cannot exceed 255 characters");
        }

        if (product.getPrice() == null) {
            throw new BusinessException("Product price is required");
        }

        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Product price cannot be negative");
        }

        if (product.getCategoryId() <= 0) {
            throw new BusinessException("Valid category ID is required");
        }
    }
}