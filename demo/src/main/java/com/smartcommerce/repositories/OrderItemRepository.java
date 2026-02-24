package com.smartcommerce.repositories;

import com.smartcommerce.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") int orderId);
}
