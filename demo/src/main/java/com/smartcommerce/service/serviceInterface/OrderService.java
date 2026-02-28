package com.smartcommerce.service.serviceInterface;

import java.sql.Timestamp;
import java.util.List;

import com.smartcommerce.dtos.request.CreateOrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;

/**
 * Service interface for Order entity
 * Defines business operations related to orders
 */
public interface OrderService {

    /**
     * Creates a new order with order items
     *
     * @param userId Order object to create
     * @param createOrderDTO List of order items
     * @return Created order with generated ID
     * @throws ResourceNotFoundException if user or product not found
     * @throws BusinessException if order creation fails
     */
    Order createOrder(int userId, CreateOrderDTO createOrderDTO);


    /**
     * Retrieves all orders (paginated)
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated orders
     */
    Page<Order> getAllOrders(Pageable pageable);

    /**
     * Retrieves all orders
     *
     * @return List of all orders
     */
    List<Order> getAllOrders();

    /**
     * Retrieves an order by ID
     *
     * @param orderId Order ID
     * @return Order object with order items
     * @throws ResourceNotFoundException if order not found
     */
    Order getOrderById(int orderId);

    /**
     * Retrieves all orders for a specific user (paginated)
     *
     * @param userId   User ID
     * @param pageable Pagination and sorting parameters
     * @return Paginated orders for the user
     * @throws ResourceNotFoundException if user not found
     */
    Page<Order> getOrdersByUserId(int userId, Pageable pageable);

    /**
     * Retrieves all orders for a specific user
     *
     * @param userId User ID
     * @return List of orders for the user
     * @throws ResourceNotFoundException if user not found
     */
    List<Order> getOrdersByUserId(int userId);

    /**
     * Updates the status of an order
     *
     * @param orderId Order ID
     * @param status New status
     * @return Updated order
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if status update fails
     */
    Order updateOrderStatus(int orderId, String status);

    /**
     * Cancels an order (sets status to 'cancelled')
     *
     * @param orderId Order ID
     * @return Cancelled order
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order cannot be cancelled
     */
    Order cancelOrder(int orderId);

    /**
     * Deletes an order and its items
     *
     * @param orderId Order ID
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order deletion fails
     */
    void deleteOrder(int orderId);

    /**
     * Gets order items for a specific order
     *
     * @param orderId Order ID
     * @return List of order items
     * @throws ResourceNotFoundException if order not found
     */
    List<OrderItem> getOrderItems(int orderId);

    /**
     * Creates an order from user's cart
     * Validates stock, calculates total, creates order and items, deducts inventory, clears cart
     *
     * @param userId User ID
     * @return Created order
     * @throws ResourceNotFoundException if user not found or cart is empty
     * @throws BusinessException if insufficient stock or order creation fails
     */
    Order checkoutFromCart(int userId);
    
    // ============================================================
    // OPTIMIZED REPORTING QUERIES - User Story 3.2
    // ============================================================
    
    /**
     * Get orders by status with items (optimized for reporting dashboards)
     * Uses composite index (status, order_date) and JOIN FETCH
     *
     * @param status Order status
     * @return List of orders with items
     */
    List<Order> getOrdersByStatus(String status);
    
    /**
     * Get user orders by status (optimized for user order history filtering)
     * Uses composite index (user_id, status) and JOIN FETCH
     *
     * @param userId User ID
     * @param status Order status
     * @return List of orders with items
     * @throws ResourceNotFoundException if user not found
     */
    List<Order> getUserOrdersByStatus(int userId, String status);
    
    /**
     * Get orders within date range (optimized for reporting)
     * Uses index on order_date and JOIN FETCH
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of orders with items in date range
     */
    List<Order> getOrdersInDateRange(Timestamp startDate, Timestamp endDate);
}
