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

    // Eagerly fetch category to avoid LazyInitializationException in GraphQL resolvers
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAllWithCategory(Sort sort);

    // Paginated methods
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.categoryName = :categoryName")
    Page<Product> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    Page<Product> searchProducts(@Param("term") String term, Pageable pageable);

    // Non-paginated methods (kept for backward compatibility)
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.categoryName = :categoryName ORDER BY p.name")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    List<Product> searchProducts(@Param("term") String term);

    @Query("SELECT p FROM Product p JOIN FETCH p.category c WHERE " +
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

}