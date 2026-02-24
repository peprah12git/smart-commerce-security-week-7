package com.smartcommerce.service.imp;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.CartItem;
import com.smartcommerce.model.Product;
import com.smartcommerce.repositories.CartItemRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.CartItemService;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;

/**
 * Service layer for CartItem entity
 * Handles business logic, validation, and orchestration of cart operations
 */
@Service
@Transactional
public class CartItemServiceImp implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryServiceInterface inventoryService;

    @Autowired
    public CartItemServiceImp(CartItemRepository cartItemRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository,
                               InventoryServiceInterface inventoryService) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Adds an item to the user's cart or updates quantity if already exists
     */
    @Override
    public CartItem addToCart(int userId, int productId, int quantity) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Validate quantity
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        // Check stock availability using Inventory service
        CartItem existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        int totalQuantity = quantity + (existingItem != null ? existingItem.getQuantity() : 0);
        
        if (!inventoryService.hasEnoughStock(productId, totalQuantity)) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        // Create or update cart item
        if (existingItem != null) {
            existingItem.setQuantity(totalQuantity);
            return cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            return cartItemRepository.save(cartItem);
        }
    }

    /**
     * Retrieves all cart items for a user (basic info)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsByUserId(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return cartItemRepository.findByUserId(userId);
    }

    /**
     * Retrieves all cart items for a user with product details
     */
    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsWithDetails(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return cartItemRepository.findByUserId(userId);
    }

    /**
     * Gets a specific cart item
     */
    @Override
    @Transactional(readOnly = true)
    public CartItem getCartItem(int userId, int productId) {
        return cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId));
    }

    /**
     * Updates the quantity of a cart item
     */
    @Override
    public CartItem updateQuantity(int userId, int productId, int quantity) {
        // Validate cart item exists
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId));

        // Validate quantity
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero. Use removeFromCart to delete items.");
        }

        // Check stock availability using Inventory service
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!inventoryService.hasEnoughStock(productId, quantity)) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        // Update quantity
        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    /**
     * Removes a specific item from the cart
     */
    @Override
    public void removeFromCart(int userId, int productId) {
        // Validate cart item exists
        if (!cartItemRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId);
        }

        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /**
     * Clears all items from a user's cart
     */
    @Override
    public void clearCart(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        cartItemRepository.deleteByUserId(userId);
    }

    /**
     * Gets the count of items in a user's cart
     */
    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return cartItemRepository.countByUserId(userId);
    }

    /**
     * Calculates the total value of a user's cart
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(int userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        BigDecimal total = cartItemRepository.calculateCartTotal(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
