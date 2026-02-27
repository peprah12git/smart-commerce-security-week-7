package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.smartcommerce.dtos.request.CreateOrderDTO;
import com.smartcommerce.dtos.request.OrderItemDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.CartItem;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;
import com.smartcommerce.model.Product;
import com.smartcommerce.model.User;
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

    private static final List<String> VALID_STATUSES = List.of(
            "pending", "confirmed", "processing", "shipped", "delivered", "cancelled"
    );

    @Autowired
    public OrderServiceImp(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           InventoryServiceInterface inventoryService,
                           CartItemService cartItemService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.cartItemService = cartItemService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Order createOrder(int userId, CreateOrderDTO createOrderDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (createOrderDTO.items() == null || createOrderDTO.items().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

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

            totalAmount = totalAmount.add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        for (OrderItem item : orderItems) {
            boolean stockReduced = inventoryService.reduceStock(
                    item.getProduct().getProductId(), item.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product: " + item.getProduct().getName());
            }
        }

        savedOrder.setOrderItems(orderItems);
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
        return getOrderById(orderId);
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

        return getOrderById(orderId);
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

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (!inventoryService.hasEnoughStock(product.getProductId(), cartItem.getQuantity())) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus("confirmed");
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItemRepository.save(orderItem);

            boolean stockReduced = inventoryService.reduceStock(product.getProductId(), cartItem.getQuantity());
            if (!stockReduced) {
                throw new BusinessException("Failed to reduce stock for product: " + product.getName());
            }
        }

        cartItemService.clearCart(userId);

        savedOrder.setOrderItems(orderItemRepository.findByOrderOrderId(savedOrder.getOrderId()));
        return savedOrder;
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findAll(pageable);

        List<Integer> orderIds = ordersPage.getContent().stream()
                .map(Order::getOrderId)
                .toList();

        if (!orderIds.isEmpty()) {
            List<Order> ordersWithItems = orderRepository.findByOrderIdsWithItems(orderIds);
            return ordersPage.map(order ->
                    ordersWithItems.stream()
                            .filter(o -> o.getOrderId() == order.getOrderId())
                            .findFirst()
                            .orElse(order)
            );
        }
        return ordersPage;
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
