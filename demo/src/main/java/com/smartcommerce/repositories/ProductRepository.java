package com.smartcommerce.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcommerce.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    // Paginated methods
    @Query("SELECT p FROM Product p WHERE p.category.categoryName = :categoryName")
    Page<Product> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    Page<Product> searchProducts(@Param("term") String term, Pageable pageable);
    
    // Non-paginated methods (kept for backward compatibility)
    @Query("SELECT p FROM Product p WHERE p.category.categoryName = :categoryName ORDER BY p.name")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);
    
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:term% OR p.description LIKE %:term%")
    List<Product> searchProducts(@Param("term") String term);
    
    List<Product> findAllByOrderByProductIdDesc();
}
