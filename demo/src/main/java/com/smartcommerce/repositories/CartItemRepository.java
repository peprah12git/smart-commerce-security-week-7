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

    List<CartItem> findByUser_UserId(int userId);

    Optional<CartItem> findByUser_UserIdAndProduct_ProductId(int userId, int productId);

    @Modifying
    @Transactional
    void deleteByUser_UserId(int userId);

    @Modifying
    @Transactional
    void deleteByUser_UserIdAndProduct_ProductId(int userId, int productId);

    @Modifying
    @Transactional
    @Query("UPDATE CartItem c SET c.quantity = :quantity WHERE c.user.userId = :userId AND c.product.productId = :productId")
    int updateQuantity(@Param("userId") int userId, @Param("productId") int productId, @Param("quantity") int quantity);

    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.user.userId = :userId")
    int countByUserId(@Param("userId") int userId);

    @Query("SELECT COALESCE(SUM(c.quantity * c.product.price), 0) FROM CartItem c WHERE c.user.userId = :userId")
    BigDecimal calculateCartTotal(@Param("userId") int userId);
}