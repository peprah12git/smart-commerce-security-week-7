package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

/**
 * Service implementation for Product entity
 * Handles business logic, validation, sorting, and filtering
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", key = "#result.productId")
    })
    public Product createProduct(Product product, Integer categoryId) {
        // Business validation
        validateProduct(product, categoryId);
        // Set category relationship
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsWithFilters(
            String sortBy,
            String sortDirection,
            ProductFilterDTO filters) {

        // Build Sort object from string parameters
        Sort sort = buildSort(sortBy, sortDirection);
        
        List<Product> products = productRepository.findAll(sort);

        if (filters != null && filters.hasFilters()) {
            products = applyFilters(products, filters);
        }

        return products;
    }

    /**
     * Apply filters to product list
     */
    private List<Product> applyFilters(List<Product> products, ProductFilterDTO filters) {
        return products.stream()
                .filter(p -> matchesCategory(p, filters.category()))
                .filter(p -> matchesPriceRange(p, filters.minPrice(), filters.maxPrice()))
                .filter(p -> matchesSearchTerm(p, filters.searchTerm()))
                .filter(p -> matchesStockStatus(filters.inStock()))
                .collect(Collectors.toList());
    }

    /**
     * Check if product matches category filter
     */
    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.trim().isEmpty()) {
            return true;
        }
        return product.getCategory() != null &&
                product.getCategory().getCategoryName().equalsIgnoreCase(category.trim());
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
        boolean matchesName = product.getName() != null &&
                product.getName().toLowerCase().contains(term);
        boolean matchesDescription = product.getDescription() != null &&
                product.getDescription().toLowerCase().contains(term);

        return matchesName || matchesDescription;
    }

    /**
     * Check if product matches stock status filter
     * Note: Stock status is now managed by Inventory table, not Product.
     * This method is kept for filter compatibility but does not filter by stock.
     */
    private boolean matchesStockStatus(Boolean inStock) {
        // Always return true since stock is managed separately in Inventory table
        // The inStock parameter is kept for future enhancement if needed
        return inStock == null || true;
    }

    /**
     * Build Spring Sort object from string parameters
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "productId"; // Default sort
        }

        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "ASC"; // Default direction
        }

        // Map user-friendly field names to entity field names
        String entityField = switch (sortBy.toLowerCase()) {
            case "name" -> "name";
            case "price" -> "price";
            case "createdat" -> "createdAt";
            case "productid", "id" -> "productId";
            default -> throw new BusinessException("Invalid sort field: " + sortBy +
                    ". Valid fields: productName, price, createdAt, productId");
        };

        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;

        return Sort.by(direction, entityField);
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
    public Product updateProduct(int productId, Product productDetails, Integer categoryId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        validateProduct(productDetails, categoryId);

        // Update category if different
        if (categoryId != null && (existingProduct.getCategory() == null || 
                existingProduct.getCategory().getCategoryId() != categoryId)) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
            existingProduct.setCategory(category);
        }

        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());

        return productRepository.save(existingProduct);
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

        // With filters, we need to fetch all with sorting, filter, then create a page
        // Note: This is less efficient than database-level filtering
        // For production, consider using Specifications or QueryDSL
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.ASC, "productId");
        List<Product> allProducts = productRepository.findAll(sort);
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

    private void validateProduct(Product product, Integer categoryId) {
        if (product == null) {
            throw new BusinessException("Product cannot be null");
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessException("Product name is required");
        }

        if (product.getName().length() > 255) {
            throw new BusinessException("Product name cannot exceed 255 characters");
        }

        if (product.getPrice() == null) {
            throw new BusinessException("Product price is required");
        }

        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Product price cannot be negative");
        }

        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException("Valid category ID is required");
        }
    }
}