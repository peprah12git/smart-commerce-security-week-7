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

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.orderId = :orderId")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Optional<Order> findByIdWithItems(@Param("orderId") int orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findAllWithItemsOrderByDateDesc();

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.user.userId = :userId " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByUserIdWithItemsOrderByDateDesc(@Param("userId") int userId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.status = :status " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByStatusWithItems(@Param("status") String status);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.user.userId = :userId AND o.status = :status " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByUserIdAndStatusWithItems(@Param("userId") int userId, @Param("status") String status);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findOrdersInDateRangeWithItems(
            @Param("startDate") java.sql.Timestamp startDate,
            @Param("endDate") java.sql.Timestamp endDate);

    @Query(value = "SELECT DISTINCT o.orderId FROM Order o " +
            "WHERE o.user.userId = :userId " +
            "ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(DISTINCT o.orderId) FROM Order o WHERE o.user.userId = :userId")
    Page<Integer> findOrderIdsByUserId(@Param("userId") int userId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "WHERE o.orderId IN :orderIds " +
            "ORDER BY o.orderDate DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Order> findByOrderIdsWithItems(@Param("orderIds") List<Integer> orderIds);

    Page<Order> findByUser_UserId(int userId, Pageable pageable);

    List<Order> findAllByOrderByOrderDateDesc();

    List<Order> findByUser_UserIdOrderByOrderDateDesc(int userId);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.orderId = :orderId")
    int updateOrderStatus(@Param("orderId") int orderId, @Param("status") String status);
}