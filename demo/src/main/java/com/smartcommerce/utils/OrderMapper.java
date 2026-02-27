package com.smartcommerce.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.smartcommerce.dtos.response.OrderItemResponse;
import com.smartcommerce.dtos.response.OrderResponse;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;

/**
 * Mapper utility class for Order entity and DTOs
 */
public class OrderMapper {

    /**
     * Converts OrderItem entity to OrderItemResponse DTO
     */
    public static OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemResponse response = new OrderItemResponse();
        response.setOrderItemId(item.getOrderItemId());
        response.setOrderId(item.getOrder().getOrderId());
        response.setProductId(item.getProduct().getProductId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());

        return response;
    }

    /**
     * Converts list of OrderItem entities to list of OrderItemResponse DTOs
     */
    public static List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(OrderMapper::toOrderItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts Order entity to OrderResponse DTO
     */
    public static OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setUserId(order.getUser().getUserId());
        response.setUserName(order.getUser().getName());
        response.setOrderDate(order.getOrderDate());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());

        // Convert order items
        List<OrderItemResponse> itemResponses = toOrderItemResponseList(order.getOrderItems());
        response.setItems(itemResponses);

        return response;
    }

    /**
     * Converts list of Order entities to list of OrderResponse DTOs
     */
    public static List<OrderResponse> toOrderResponseList(List<Order> orders) {
        if (orders == null) {
            return new ArrayList<>();
        }

        return orders.stream()
                .map(OrderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }
}
