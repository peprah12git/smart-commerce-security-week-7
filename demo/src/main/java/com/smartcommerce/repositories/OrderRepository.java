package com.smartcommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.model.Order;

import jakarta.persistence.QueryHint;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    // ============================================================
    // OPTIMIZED QUERIES WITH JOIN FETCH
    // Purpose: Eliminate N+1 query problems for order history
    // ============================================================
    
    /**
     * Optimized: Fetch order with all order items in single query
     * Benefits: Eliminates N+1 query problem, reduces DB round-trips
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.orderId = :orderId")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Optional<Order> findByIdWithItems(@Param("orderId") int orderId);
    
    /**
     * Optimized: Fetch all orders with items, sorted by date (for reporting)
     * Benefits: Single query instead of N+1, uses composite index
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findAllWithItemsOrderByDateDesc();
    
    /**
     * Optimized: Fetch user orders with items in single query
     * Benefits: Uses composite index (user_id, order_date), eliminates N+1
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.userId = :userId " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByUserIdWithItemsOrderByDateDesc(@Param("userId") int userId);
    
    /**
     * Optimized: Fetch orders by status with items (for reporting dashboards)
     * Benefits: Uses composite index (status, order_date)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.status = :status " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByStatusWithItems(@Param("status") String status);
    
    /**
     * Optimized: Fetch user orders by status with items
     * Benefits: Uses composite index (user_id, status)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.userId = :userId AND o.status = :status " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByUserIdAndStatusWithItems(@Param("userId") int userId, @Param("status") String status);
    
    /**
     * Optimized: Order reporting query with date range
     * Benefits: Uses composite index (status, order_date)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findOrdersInDateRangeWithItems(
        @Param("startDate") java.sql.Timestamp startDate, 
        @Param("endDate") java.sql.Timestamp endDate);
    
    /**
     * Optimized: Paginated query with JOIN FETCH for order list
     * Note: For pagination with JOIN FETCH, fetch IDs first then fetch entities
     */
    @Query(value = "SELECT DISTINCT o.orderId FROM Order o " +
                   "WHERE o.userId = :userId " +
                   "ORDER BY o.orderDate DESC",
           countQuery = "SELECT COUNT(DISTINCT o.orderId) FROM Order o WHERE o.userId = :userId")
    Page<Integer> findOrderIdsByUserId(@Param("userId") int userId, Pageable pageable);
    
    /**
     * Optimized: Fetch orders by IDs with items (for pagination)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "WHERE o.orderId IN :orderIds " +
           "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByOrderIdsWithItems(@Param("orderIds") List<Integer> orderIds);
    
    // ============================================================
    // LEGACY METHODS (kept for backward compatibility)
    // ============================================================
    
    // Paginated methods
    Page<Order> findByUserId(int userId, Pageable pageable);
    
    // Non-paginated methods
    List<Order> findAllByOrderByOrderDateDesc();
    
    List<Order> findByUserIdOrderByOrderDateDesc(int userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.orderId = :orderId")
    int updateOrderStatus(@Param("orderId") int orderId, @Param("status") String status);
}
