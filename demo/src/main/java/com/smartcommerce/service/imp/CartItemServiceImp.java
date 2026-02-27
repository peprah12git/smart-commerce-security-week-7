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
import com.smartcommerce.model.User;
import com.smartcommerce.repositories.CartItemRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.repositories.UserRepository;
import com.smartcommerce.service.serviceInterface.CartItemService;
import com.smartcommerce.service.serviceInterface.InventoryServiceInterface;

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

    @Override
    public CartItem addToCart(int userId, int productId, int quantity) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        CartItem existingItem = cartItemRepository.findByUser_UserIdAndProduct_ProductId(userId, productId).orElse(null);
        int totalQuantity = quantity + (existingItem != null ? existingItem.getQuantity() : 0);

        if (!inventoryService.hasEnoughStock(productId, totalQuantity)) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        if (existingItem != null) {
            existingItem.setQuantity(totalQuantity);
            return cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUser(existingUser);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            return cartItemRepository.save(cartItem);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsByUserId(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return cartItemRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsWithDetails(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        // FIXED: findByUser_UserId
        return cartItemRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CartItem getCartItem(int userId, int productId) {
        return cartItemRepository.findByUser_UserIdAndProduct_ProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId));
    }

    @Override
    public CartItem updateQuantity(int userId, int productId, int quantity) {
        CartItem cartItem = cartItemRepository.findByUser_UserIdAndProduct_ProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId));

        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero. Use removeFromCart to delete items.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!inventoryService.hasEnoughStock(productId, quantity)) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    @Override
    public void removeFromCart(int userId, int productId) {
        if (!cartItemRepository.findByUser_UserIdAndProduct_ProductId(userId, productId).isPresent()) {
            throw new ResourceNotFoundException("CartItem", "userId=" + userId + ", productId", productId);
        }
        cartItemRepository.deleteByUser_UserIdAndProduct_ProductId(userId, productId);
    }

    @Override
    public void clearCart(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        cartItemRepository.deleteByUser_UserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return cartItemRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        BigDecimal total = cartItemRepository.calculateCartTotal(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
}