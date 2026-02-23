package com.smartcommerce.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    // Paginated methods
    Page<Order> findByUserId(int userId, Pageable pageable);
    
    // Non-paginated methods (kept for backward compatibility)
    List<Order> findAllByOrderByOrderDateDesc();
    
    List<Order> findByUserIdOrderByOrderDateDesc(int userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.orderId = :orderId")
    int updateOrderStatus(@Param("orderId") int orderId, @Param("status") String status);
}
