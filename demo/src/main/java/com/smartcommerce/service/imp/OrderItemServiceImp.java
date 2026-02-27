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
        Order order = orderRepository.findById(orderItem.getOrder().getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderItem.getOrder().getOrderId()));

        Product product = productRepository.findById(orderItem.getProduct().getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", orderItem.getProduct().getProductId()));

        if (orderItem.getUnitPrice() == null) {
            orderItem.setUnitPrice(product.getPrice());
        }

        orderItem.setOrder(order);
        orderItem.setProduct(product);

        return orderItemRepository.save(orderItem);
    }

    @Override
    public OrderItem addOrderItem(int orderId, int productId, int quantity, BigDecimal unitPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);

        return orderItemRepository.save(orderItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrderId(int orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        return orderItemRepository.findByOrderOrderId(orderId);
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

        OrderItem orderItem = getOrderItemById(orderItemId);
        orderItem.setQuantity(quantity);

        return orderItemRepository.save(orderItem);
    }

    @Override
    public void deleteOrderItem(int orderItemId) {
        if (!orderItemRepository.existsById(orderItemId)) {
            throw new ResourceNotFoundException("OrderItem", "id", orderItemId);
        }
        orderItemRepository.deleteById(orderItemId);
    }

    @Override
    public void deleteOrderItemsByOrderId(int orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        orderItemRepository.deleteAll(items);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSubtotal(int orderItemId) {
        OrderItem orderItem = getOrderItemById(orderItemId);
        return orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity()));
    }
}
