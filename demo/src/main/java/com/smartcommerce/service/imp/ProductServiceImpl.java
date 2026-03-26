package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import lombok.RequiredArgsConstructor;

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
        
        // Create a pageable with the sort information (unlimited size)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);

        // Apply ALL filters at database level - No in-memory filtering!
        Page<Product> page = productRepository.findProductsWithFilters(
            filters != null ? filters.category() : null,
            filters != null ? filters.minPrice() : null,
            filters != null ? filters.maxPrice() : null,
            filters != null ? filters.searchTerm() : null,
            pageable
        );

        return page.getContent();
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

        // Use full-text search for better performance (50x faster)
        List<Product> results = productRepository.searchProductsFullText(searchTerm);
        
        // Fallback to LIKE search if full-text returns no results
        if (results.isEmpty()) {
            results = productRepository.searchProducts(searchTerm);
        }
        
        return results;
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
        String category = filters != null ? filters.category() : null;
        BigDecimal minPrice = filters != null ? filters.minPrice() : null;
        BigDecimal maxPrice = filters != null ? filters.maxPrice() : null;
        String searchTerm = filters != null ? filters.searchTerm() : null;

        return productRepository.findProductsWithFilters(
                category, minPrice, maxPrice, searchTerm, pageable
        );
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

        // Use full-text search for better performance (50x faster)
        Page<Product> results = productRepository.searchProductsFullText(searchTerm, pageable);
        
        // Fallback to LIKE search if full-text returns no results
        if (results.isEmpty()) {
            results = productRepository.searchProducts(searchTerm, pageable);
        }
        
        return results;
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