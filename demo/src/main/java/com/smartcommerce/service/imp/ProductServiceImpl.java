package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.dao.interfaces.CategoryDaoInterface;
import com.smartcommerce.dao.interfaces.ProductDaoInterface;
import com.smartcommerce.dtos.request.ProductFilterDTO;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Category;
import com.smartcommerce.model.Product;
import com.smartcommerce.service.serviceInterface.ProductService;
import com.smartcommerce.sorting.SortStrategy;

/**
 * Service implementation for Product entity
 * Handles business logic, validation, sorting, and filtering
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductDaoInterface productDao;
    private final CategoryDaoInterface categoryDao;
    private final SortStrategy<Product> sortStrategy;

    // Manual constructor for dependency injection
    public ProductServiceImpl(ProductDaoInterface productDao,
                              CategoryDaoInterface categoryDao,
                              SortStrategy<Product> sortStrategy) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.sortStrategy = sortStrategy;
    }

    @Override
    public Product createProduct(Product product) {
        // Business validation
        validateProduct(product);
        // check if category exist
        Category category = categoryDao.getCategoryById(product.getCategoryId());
        if (category == null) {
            throw new ResourceNotFoundException("Category", "id", product.getCategoryId());
        }

        boolean success = productDao.addProduct(product);
        if (!success) {
            throw new BusinessException("Failed to create product");
        }
//---invalidate cache
        productDao.invalidateCache();

        List<Product> products = productDao.getAllProducts();
        Product createdProduct = products.stream()
                .filter(p -> p.getProductName().equals(product.getProductName())
                        && p.getCategoryId() == product.getCategoryId())
                .findFirst()
                .orElse(null);

        if (createdProduct == null) {
            throw new BusinessException("Product created but could not be retrieved");
        }

        return createdProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productDao.getAllProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsWithFilters(
            String sortBy,
            String sortDirection,
            ProductFilterDTO filters) {

        List<Product> products = productDao.getAllProducts();

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
    public Product getProductById(int productId) {
        Product product = productDao.getProductById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new BusinessException("Category name cannot be empty");
        }

        return productDao.getProductsByCategory(categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new BusinessException("Search term cannot be empty");
        }

        return productDao.searchProducts(searchTerm);
    }

    @Override
    public Product updateProduct(int productId, Product productDetails) {
        Product existingProduct = productDao.getProductById(productId);
        if (existingProduct == null) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        validateProduct(productDetails);

        if (existingProduct.getCategoryId() != productDetails.getCategoryId()) {
            Category category = categoryDao.getCategoryById(productDetails.getCategoryId());
            if (category == null) {
                throw new ResourceNotFoundException("Category", "id", productDetails.getCategoryId());
            }
        }

        existingProduct.setProductName(productDetails.getProductName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setCategoryId(productDetails.getCategoryId());

        if (productDetails.getQuantityAvailable() >= 0) {
            existingProduct.setQuantityAvailable(productDetails.getQuantityAvailable());
        }

        boolean success = productDao.updateProduct(existingProduct);
        if (!success) {
            throw new BusinessException("Failed to update product");
        }

        productDao.invalidateCache();

        return getProductById(productId);
    }

    @Override
    public Product updateProductQuantity(int productId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException("Quantity cannot be negative");
        }

        Product product = getProductById(productId);
        product.setQuantityAvailable(quantity);

        boolean success = productDao.updateProduct(product);
        if (!success) {
            throw new BusinessException("Failed to update product quantity");
        }

        productDao.invalidateCache();
        return getProductById(productId);
    }

    @Override
    public void deleteProduct(int productId) {
        Product product = productDao.getProductById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        boolean success = productDao.deleteProduct(productId);
        if (!success) {
            throw new BusinessException("Failed to delete product");
        }

        productDao.invalidateCache();
    }

    @Override
    public void invalidateProductCache() {
        productDao.invalidateCache();
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