package com.smartcommerce.service.serviceInterface;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.smartcommerce.dtos.request.ProductFilterDTO;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Product;

/**
 * Service interface for Product entity
 * Defines business operations related to products
 */
public interface ProductService {

    /**
     * Creates a new product
     *
     * @param product Product object to create
     * @return Created product
     * @throws ResourceNotFoundException if category not found
     * @throws BusinessException         if product creation fails
     */
    Product createProduct(Product product);

    /**
     * Retrieves all products
     *
     * @return List of all products
     */
    List<Product> getAllProducts();

    /**
     * Retrieves products with sorting and filtering (paginated)
     *
     * @param pageable Pagination and sorting parameters
     * @param filters  Filter criteria
     * @return Paginated filtered products
     */
    Page<Product> getProductsWithFilters(Pageable pageable, ProductFilterDTO filters);

    /**
     * Retrieves products with sorting and filtering
     *
     * @param sortBy       Field to sort by (productName, price, createdAt, etc.)
     * @param sortDirection Sort direction (ASC or DESC)
     * @param filters      Filter criteria
     * @return Filtered list of products
     */
    List<Product> getProductsWithFilters(
            String sortBy,
            String sortDirection,
            ProductFilterDTO filters
    );

    /**
     * Retrieves a product by ID
     *
     * @param productId Product ID
     * @return Product object
     * @throws ResourceNotFoundException if product not found
     */
    Product getProductById(int productId);

    /**
     * Retrieves products by category name (paginated)
     *
     * @param categoryName Category name
     * @param pageable     Pagination and sorting parameters
     * @return Paginated products in the category
     * @throws BusinessException if category name is invalid
     */
    Page<Product> getProductsByCategory(String categoryName, Pageable pageable);

    /**
     * Retrieves products by category name
     *
     * @param categoryName Category name
     * @return List of products in the category
     * @throws BusinessException if category name is invalid
     */
    List<Product> getProductsByCategory(String categoryName);

    /**
     * Searches for products by name or description (paginated)
     *
     * @param searchTerm Search term
     * @param pageable   Pagination and sorting parameters
     * @return Paginated matching products
     * @throws BusinessException if search term is invalid
     */
    Page<Product> searchProducts(String searchTerm, Pageable pageable);

    /**
     * Searches for products by name or description
     *
     * @param searchTerm Search term
     * @return List of matching products
     * @throws BusinessException if search term is invalid
     */
    List<Product> searchProducts(String searchTerm);

    /**
     * Updates an existing product
     *
     * @param productId      Product ID to update
     * @param productDetails Updated product details
     * @return Updated product
     * @throws ResourceNotFoundException if product or category not found
     * @throws BusinessException         if update fails
     */
    Product updateProduct(int productId, Product productDetails);

    /**
     * Updates product quantity
     *
     * @param productId Product ID
     * @param quantity  New quantity
     * @return Updated product
     * @throws ResourceNotFoundException if product not found
     * @throws BusinessException         if quantity is invalid or update fails
     */
    Product updateProductQuantity(int productId, int quantity);

    /**
     * Deletes a product
     *
     * @param productId Product ID to delete
     * @throws ResourceNotFoundException if product not found
     * @throws BusinessException         if deletion fails
     */
    void deleteProduct(int productId);

    /**
     * Invalidates the product cache
     */
    void invalidateProductCache();
}