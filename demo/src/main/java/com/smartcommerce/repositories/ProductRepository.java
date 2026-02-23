package com.smartcommerce.repositories;

import com.smartcommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    @Query("SELECT p FROM Product p JOIN Category c ON p.categoryId = c.categoryId WHERE c.categoryName = :categoryName ORDER BY p.productName")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);
    
    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:term% OR p.description LIKE %:term%")
    List<Product> searchProducts(@Param("term") String term);
    
    List<Product> findAllByOrderByProductIdDesc();
}
