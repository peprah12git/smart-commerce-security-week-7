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
import com.smartcommerce.repositories.OrderItemRepository;
import com.smartcommerce.repositories.OrderRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.service.serviceInterface.OrderItemService;

/**
 * Service layer for OrderItem entity
 * Handles business logic, validation, and orchestration of order item operations
 */
@Service
@Transactional
public class OrderItemServiceImp implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderItemServiceImp(OrderItemRepository orderItemRepository,
                               OrderRepository orderRepository,
                               ProductRepository productRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public OrderItem addOrderItem(OrderItem orderItem) {
        // Validate order exists
        Order order = orderRepository.findById(orderItem.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderItem.getOrderId()));

        // Validate product exists
        Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", orderItem.getProductId()));

        // Set unit price from product if not provided
        if (orderItem.getUnitPrice() == null) {
            orderItem.setUnitPrice(product.getPrice());
        }

        // Set product name for convenience
        orderItem.setProductName(product.getProductName());

        return orderItemRepository.save(orderItem);
    }

    @Override
    public OrderItem addOrderItem(int orderId, int productId, int quantity, BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem(orderId, productId, quantity, unitPrice);
        return addOrderItem(orderItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrderId(int orderId) {
        // Validate order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItem getOrderItemById(int orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));
    }

    @Override
    public OrderItem updateOrderItemQuantity(int orderItemId, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }

        // Get order item
        OrderItem orderItem = getOrderItemById(orderItemId);

        // Update quantity
        orderItem.setQuantity(quantity);

        // Recalculate subtotal
        orderItem.setSubtotal(orderItem.getUnitPrice().multiply(new BigDecimal(quantity)));

        return orderItemRepository.save(orderItem);
    }

    @Override
    public void deleteOrderItem(int orderItemId) {
        // Verify order item exists
        if (!orderItemRepository.existsById(orderItemId)) {
            throw new ResourceNotFoundException("OrderItem", "id", orderItemId);
        }

        orderItemRepository.deleteById(orderItemId);
    }

    @Override
    public void deleteOrderItemsByOrderId(int orderId) {
        // Validate order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        // Get all items for the order and delete them
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        orderItemRepository.deleteAll(items);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSubtotal(int orderItemId) {
        OrderItem orderItem = getOrderItemById(orderItemId);
        return orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity()));
    }
}
