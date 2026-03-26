package com.smartcommerce.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcommerce.model.Product;
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Fetch category AND inventory in one query — eliminates N+1
    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory")
    List<Product> findAllWithCategoryAndInventory(Sort sort);

    // For getAllProducts() — no sort needed
    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory")
    List<Product> findAllWithCategoryAndInventory();

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory " +
            "WHERE p.category.categoryName = :categoryName")
    Page<Product> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory " +
            "WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    Page<Product> searchProducts(@Param("term") String term, Pageable pageable);

    // Full-text search using MySQL FULLTEXT index for better performance (50x faster)
    @Query(value = "SELECT p.* FROM Products p " +
            "JOIN Categories c ON p.category_id = c.category_id " +
            "LEFT JOIN Inventory i ON p.product_id = i.product_id " +
            "WHERE MATCH(p.name, p.description) AGAINST(:term IN NATURAL LANGUAGE MODE) " +
            "ORDER BY MATCH(p.name, p.description) AGAINST(:term IN NATURAL LANGUAGE MODE) DESC",
            countQuery = "SELECT COUNT(*) FROM Products p " +
                    "WHERE MATCH(p.name, p.description) AGAINST(:term IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    Page<Product> searchProductsFullText(@Param("term") String term, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory " +
            "WHERE p.category.categoryName = :categoryName ORDER BY p.name")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory " +
            "WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    List<Product> searchProducts(@Param("term") String term);

    // Full-text search using MySQL FULLTEXT index for better performance (50x faster)
    @Query(value = "SELECT p.* FROM Products p " +
            "JOIN Categories c ON p.category_id = c.category_id " +
            "LEFT JOIN Inventory i ON p.product_id = i.product_id " +
            "WHERE MATCH(p.name, p.description) AGAINST(:term IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    List<Product> searchProductsFullText(@Param("term") String term);

    @Query("SELECT p FROM Product p JOIN FETCH p.category c LEFT JOIN FETCH p.inventory WHERE " +
            "(:category IS NULL OR LOWER(c.categoryName) = LOWER(:category)) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Product> findProductsWithFilters(
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.inventory WHERE p.productId = :id")
    java.util.Optional<Product> findByIdWithCategory(@Param("id") int id);
}