package com.smartcommerce.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.smartcommerce.dtos.response.CartItemResponse;
import com.smartcommerce.model.CartItem;

/**
 * Mapper utility class for CartItem entity and DTOs
 */
public class CartItemMapper {

    /**
     * Converts CartItem entity to CartItemResponse DTO
     */
    public static CartItemResponse toCartItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(item.getCartItemId());
        response.setUserId(item.getUser().getUserId());
        response.setProductId(item.getProductId());
        response.setProductName(item.getProduct() != null ? item.getProduct().getName() : null);
        response.setProductPrice(item.getProduct() != null ? item.getProduct().getPrice() : null);
        response.setProductDescription(item.getProduct() != null ? item.getProduct().getDescription() : null);
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getProduct().getPrice());
        response.setAddedAt(item.getAddedAt());
        response.setUpdatedAt(item.getUpdatedAt());

        return response;
    }

    /**
     * Converts list of CartItem entities to list of CartItemResponse DTOs
     */
    public static List<CartItemResponse> toCartItemResponseList(List<CartItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(CartItemMapper::toCartItemResponse)
                .collect(Collectors.toList());
    }
}
