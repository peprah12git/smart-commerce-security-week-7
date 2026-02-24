package com.smartcommerce.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.model.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUserId(int userId);
    
    Optional<CartItem> findByUserIdAndProductId(int userId, int productId);
    
    @Modifying
    @Transactional
    void deleteByUserId(int userId);
    
    @Modifying
    @Transactional
    void deleteByUserIdAndProductId(int userId, int productId);
    
    @Modifying
    @Transactional
    @Query("UPDATE CartItem c SET c.quantity = :quantity WHERE c.userId = :userId AND c.productId = :productId")
    int updateQuantity(@Param("userId") int userId, @Param("productId") int productId, @Param("quantity") int quantity);
    
    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.userId = :userId")
    int countByUserId(@Param("userId") int userId);
    
    @Query("SELECT COALESCE(SUM(c.quantity * p.price), 0) FROM CartItem c, Product p WHERE c.productId = p.productId AND c.userId = :userId")
    BigDecimal calculateCartTotal(@Param("userId") int userId);
}
