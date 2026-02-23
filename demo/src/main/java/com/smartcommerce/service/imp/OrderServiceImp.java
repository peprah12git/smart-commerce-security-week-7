package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;
import com.smartcommerce.model.Product;
import com.smartcommerce.model.User;
import com.smartcommerce.repositories.OrderItemRepository;
import com.smartcommerce.repositories.OrderRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;
import com.smartcommerce.service.serviceInterface.OrderService;

/**
 * Service layer for Order entity
 * Handles business logic, validation, and orchestration of order operations
 */
@Service
@Transactional
public class OrderServiceImp implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryServiceInterface inventoryService;
    private final com.smartcommerce.service.serviceInterface.CartItemService cartItemService;

    // Valid order statuses
    private static final List<String> VALID_STATUSES = List.of(
            "pending", "confirmed", "processing", "shipped", "delivered", "cancelled"
    );

    @Autowired
    public OrderServiceImp(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           InventoryServiceInterface inventoryService,
                           com.smartcommerce.service.serviceInterface.CartItemService cartItemService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.cartItemService = cartItemService;
    }

    @Override
    public Order createOrder(Order order, List<OrderItem> orderItems) {
        // Validate user exists
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", order.getUserId()));

        // Validate order items
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        // Calculate total and validate products
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

            // Check stock availability
            if (product.getQuantityAvailable() < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getProductName() +
                        ". Available: " + product.getQuantityAvailable() + ", Requested: " + item.getQuantity());
            }

            // Set unit price from product if not provided
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(product.getPrice());
            }

            // Calculate subtotal
            BigDecimal subtotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // Set order total and default status
        order.setTotalAmount(totalAmount);
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("pending");
        }

        // Create order
        Order savedOrder = orderRepository.save(order);

        // Add order items
        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getOrderId());
            orderItemRepository.save(item);
        }

        // Reduce inventory for ordered items
        for (OrderItem item : orderItems) {
            boolean stockReduced = inventoryService.reduceStock(item.getProductId(), item.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product ID: " + item.getProductId() + 
                        ". Order creation failed.");
            }
        }

        // Set order items and return
        savedOrder.setOrderItems(orderItems);
        savedOrder.setUserName(user.getName());
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        // Load order items for each order
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            order.setOrderItems(items);
        }
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Load order items
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        order.setOrderItems(items);

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        // Load order items for each order
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            order.setOrderItems(items);
        }
        return orders;
    }

    @Override
    public Order updateOrderStatus(int orderId, String status) {
        // Validate order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate status
        String normalizedStatus = status.toLowerCase().trim();
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException("Invalid order status: " + status +
                    ". Valid statuses are: " + String.join(", ", VALID_STATUSES));
        }

        // Cannot update cancelled orders
        if ("cancelled".equals(order.getStatus())) {
            throw new BusinessException("Cannot update status of a cancelled order");
        }

        // Update status
        order.setStatus(normalizedStatus);
        orderRepository.save(order);

        // Return updated order
        return getOrderById(orderId);
    }

    @Override
    public Order cancelOrder(int orderId) {
        // Validate order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check if order can be cancelled
        String currentStatus = order.getStatus().toLowerCase();
        if ("shipped".equals(currentStatus) || "delivered".equals(currentStatus)) {
            throw new BusinessException("Cannot cancel order that is already " + currentStatus);
        }

        if ("cancelled".equals(currentStatus)) {
            throw new BusinessException("Order is already cancelled");
        }

        // Get order items to restore inventory
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // Update status to cancelled
        order.setStatus("cancelled");
        orderRepository.save(order);

        // Restore inventory for cancelled order items
        for (OrderItem item : orderItems) {
            boolean stockRestored = inventoryService.addStock(item.getProductId(), item.getQuantity());
            if (!stockRestored) {
                // Log warning but don't fail the cancellation
                System.err.println("Warning: Failed to restore stock for product ID: " + item.getProductId() + 
                        " during order cancellation");
            }
        }

        // Return updated order
        return getOrderById(orderId);
    }

    @Override
    public void deleteOrder(int orderId) {
        // Validate order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        // Delete order items first (due to foreign key constraint)
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        orderItemRepository.deleteAll(items);

        // Delete order
        orderRepository.deleteById(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(int orderId) {
        // Validate order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    public Order checkoutFromCart(int userId) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Fetch cart items with product details
        List<com.smartcommerce.model.CartItem> cartItems = cartItemService.getCartItemsWithDetails(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        // Validate stock and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (com.smartcommerce.model.CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProductId()));

            if (product.getQuantityAvailable() < cartItem.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getProductName() +
                        ". Available: " + product.getQuantityAvailable() + ", Requested: " + cartItem.getQuantity());
            }

            BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // Create Order with CONFIRMED status
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("confirmed");
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Create OrderItems and deduct inventory
        for (com.smartcommerce.model.CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProductId()));
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            orderItemRepository.save(orderItem);

            boolean stockReduced = inventoryService.reduceStock(cartItem.getProductId(), cartItem.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product ID: " + cartItem.getProductId());
            }
        }

        // Clear cart
        cartItemService.clearCart(userId);

        // Return order with items
        savedOrder.setUserName(user.getName());
        savedOrder.setOrderItems(orderItemRepository.findByOrderId(savedOrder.getOrderId()));
        return savedOrder;
    }
}
