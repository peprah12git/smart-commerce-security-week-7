package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.dtos.request.CreateOrderDTO;
import com.smartcommerce.dtos.request.OrderItemDTO;
import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.CartItem;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;
import com.smartcommerce.model.Product;
import com.smartcommerce.model.User;
import com.smartcommerce.notification.events.OrderNotificationEvent;
import com.smartcommerce.notification.OrderNotificationType;
import com.smartcommerce.repositories.OrderItemRepository;
import com.smartcommerce.repositories.OrderRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.CartItemService;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;
import com.smartcommerce.service.serviceInterface.OrderService;

@Service
@Transactional
public class OrderServiceImp implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryServiceInterface inventoryService;
    private final CartItemService cartItemService;
    private final ApplicationEventPublisher eventPublisher;
    private final InvoiceService invoiceService;

    private static final List<String> VALID_STATUSES = List.of(
            "pending", "confirmed", "processing", "shipped", "delivered", "cancelled"
    );

    @Autowired
    public OrderServiceImp(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           InventoryServiceInterface inventoryService,
                           CartItemService cartItemService,
                           ApplicationEventPublisher eventPublisher,
                           InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.cartItemService = cartItemService;
        this.eventPublisher = eventPublisher;
        this.invoiceService = invoiceService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Order createOrder(int userId, CreateOrderDTO createOrderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (createOrderDTO.items() == null || createOrderDTO.items().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }
// initialize order
        Order order = new Order();
        order.setUser(user);
        order.setStatus("pending");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO itemDTO : createOrderDTO.items()) {
            Product product = productRepository.findById(itemDTO.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDTO.productId()));

            if (!inventoryService.hasEnoughStock(itemDTO.productId(), itemDTO.quantity())) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemDTO.quantity());
            item.setUnitPrice(itemDTO.unitPrice() != null ? itemDTO.unitPrice() : product.getPrice());
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            totalAmount = totalAmount.add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Set order reference for all items and save as batch (not sequential)
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);  //  Batch operation instead of individual saves

        for (OrderItem item : orderItems) {
            boolean stockReduced = inventoryService.reduceStock(
                    item.getProduct().getProductId(), item.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product: " + item.getProduct().getName());
            }
        }

        savedOrder.setOrderItems(orderItems);
        publishOrderNotification(savedOrder, OrderNotificationType.ORDER_CREATED); // call publishNotification and pass in the saved order.

        // Async invoice generation with non-blocking error handling
        invoiceService.generateInvoiceAsync(savedOrder)
                .handle((filePath, ex) -> {
                    if (ex != null) {
                        System.err.println("Invoice generation failed for order "
                                + savedOrder.getOrderId() + ": " + ex.getMessage());
                    }
                    return null;
                });

        return savedOrder;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllWithItemsOrderByDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(int orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return orderRepository.findByUserIdWithItemsOrderByDateDesc(userId);
    }

    @Override
    public Order updateOrderStatus(int orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        String normalizedStatus = status.toLowerCase().trim();
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException("Invalid order status: " + status +
                    ". Valid statuses are: " + String.join(", ", VALID_STATUSES));
        }

        if ("cancelled".equals(order.getStatus())) {
            throw new BusinessException("Cannot update status of a cancelled order");
        }

        order.setStatus(normalizedStatus);
        orderRepository.save(order);
        Order updatedOrder = getOrderById(orderId);
        publishOrderNotification(updatedOrder, OrderNotificationType.ORDER_STATUS_UPDATED);
        return updatedOrder;
    }

    @Override
    public Order cancelOrder(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        String currentStatus = order.getStatus().toLowerCase();
        if ("shipped".equals(currentStatus) || "delivered".equals(currentStatus)) {
            throw new BusinessException("Cannot cancel order that is already " + currentStatus);
        }

        if ("cancelled".equals(currentStatus)) {
            throw new BusinessException("Order is already cancelled");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(orderId);

        order.setStatus("cancelled");
        orderRepository.save(order);

        for (OrderItem item : orderItems) {
            boolean stockRestored = inventoryService.addStock(
                    item.getProduct().getProductId(), item.getQuantity());
            if (!stockRestored) {
                System.err.println("Warning: Failed to restore stock for product ID: "
                        + item.getProduct().getProductId() + " during order cancellation");
            }
        }

        Order cancelledOrder = getOrderById(orderId);
        publishOrderNotification(cancelledOrder, OrderNotificationType.ORDER_CANCELLED);
        return cancelledOrder;
    }

    @Override
    public void deleteOrder(int orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        orderItemRepository.deleteAll(items);
        orderRepository.deleteById(orderId);
    }

    @Override
    public List<OrderItem> getOrderItems(int orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        return orderItemRepository.findByOrderOrderId(orderId);
    }

    @Override
    public Order checkoutFromCart(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<CartItem> cartItems = cartItemService.getCartItemsWithDetails(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        // Validate stock for ALL items first, before touching anything
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (!inventoryService.hasEnoughStock(product.getProductId(), cartItem.getQuantity())) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }
        }

        // Build order
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setStatus("confirmed");
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Build all OrderItems in memory, then batch save
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(savedOrder);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(product.getPrice());
                    orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                    return orderItem;
                })
                .toList();

        orderItemRepository.saveAll(orderItems); // single batch INSERT

        // Reduce stock for all items
        for (CartItem cartItem : cartItems) {
            boolean stockReduced = inventoryService.reduceStock(
                    cartItem.getProduct().getProductId(), cartItem.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product: "
                        + cartItem.getProduct().getName());
            }
        }

        cartItemService.clearCart(userId);

        // No need to re-fetch — set what we already have
        savedOrder.setOrderItems(orderItems);
        publishOrderNotification(savedOrder, OrderNotificationType.ORDER_CHECKOUT_COMPLETED);

        // Async invoice generation with non-blocking error handling
        invoiceService.generateInvoiceAsync(savedOrder)
                .handle((filePath, ex) -> {
                    if (ex != null) {
                        System.err.println("Invoice generation failed for order "
                                + savedOrder.getOrderId() + ": " + ex.getMessage());
                    }
                    return null;
                });
        return savedOrder;
    }

    private void publishOrderNotification(Order order, OrderNotificationType type) {
        if (order == null || order.getUser() == null) {
            return;
        }

        OrderNotificationEvent event = new OrderNotificationEvent(
                order.getOrderId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderDate(),
                type
        );

        eventPublisher.publishEvent(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Integer> orderIdsPage = orderRepository.findAll(pageable)
                .map(Order::getOrderId);

        List<Integer> orderIds = orderIdsPage.getContent();

        if (!orderIds.isEmpty()) {
            List<Order> ordersWithItems = orderRepository.findByOrderIdsWithItems(orderIds);
            return orderIdsPage.map(orderId ->
                    ordersWithItems.stream()
                            .filter(o -> o.getOrderId() == orderId)
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId))
            );
        }
        return Page.empty(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUserId(int userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Page<Integer> orderIdsPage = orderRepository.findOrderIdsByUserId(userId, pageable);

        if (!orderIdsPage.isEmpty()) {
            List<Order> ordersWithItems = orderRepository.findByOrderIdsWithItems(orderIdsPage.getContent());
            return orderIdsPage.map(orderId ->
                    ordersWithItems.stream()
                            .filter(o -> o.getOrderId() == orderId)
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId))
            );
        }

        return Page.empty(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatusWithItems(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getUserOrdersByStatus(int userId, String status) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return orderRepository.findByUserIdAndStatusWithItems(userId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersInDateRange(java.sql.Timestamp startDate, java.sql.Timestamp endDate) {
        return orderRepository.findOrdersInDateRangeWithItems(startDate, endDate);
    }
}
