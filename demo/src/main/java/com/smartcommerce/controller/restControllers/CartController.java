package com.smartcommerce.controller.restControllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.AddToCartDTO;
import com.smartcommerce.dtos.request.UpdateCartItemDTO;
import com.smartcommerce.dtos.response.CartItemResponse;
import com.smartcommerce.dtos.response.CartResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.CartItem;
import com.smartcommerce.service.serviceInterface.CartItemService;
import com.smartcommerce.utils.CartItemMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Shopping Cart management
 * Handles HTTP requests for cart CRUD operations
 * Base URL: /api/carts
 */
@RestController
@RequestMapping("/api/carts")
@Tag(name = "Carts", description = "Shopping cart management API — add, update, remove items and view cart")
public class CartController {

    private final CartItemService cartItemService;

    public CartController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    /**
     * Add item to cart
     * POST /api/cart/items
     */
    @Operation(summary = "Add item to cart", description = "Adds a product to the user's cart or updates quantity if already exists")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added to cart successfully",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(
            @Valid @RequestBody AddToCartDTO addToCartDTO,
            @RequestAttribute("userId") Integer userId) {

        CartItem cartItem = cartItemService.addToCart(
                userId,
                addToCartDTO.productId(),
                addToCartDTO.quantity()
        );

        CartItemResponse response = CartItemMapper.toCartItemResponse(cartItem);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get user's cart with all items and totals
     * GET /api/cart/user/{userId}
     */
    @Operation(summary = "Get user's cart", description = "Retrieves the user's complete cart with all items, count, and total")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<CartResponse> getUserCart(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        List<CartItem> cartItems = cartItemService.getCartItemsWithDetails(authenticatedUserId);
        List<CartItemResponse> itemResponses = CartItemMapper.toCartItemResponseList(cartItems);
        int itemCount = cartItemService.getCartItemCount(authenticatedUserId);
        BigDecimal total = cartItemService.getCartTotal(authenticatedUserId);

        CartResponse response = new CartResponse(authenticatedUserId, itemResponses, itemCount, total);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all cart items for a user (with product details)
     * GET /api/cart/user/{userId}/items
     */
    @Operation(summary = "Get cart items", description = "Retrieves all items in the user's cart with product details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart items retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}/items")
    public ResponseEntity<List<CartItemResponse>> getCartItems(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        List<CartItem> cartItems = cartItemService.getCartItemsWithDetails(authenticatedUserId);
        List<CartItemResponse> response = CartItemMapper.toCartItemResponseList(cartItems);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific cart item
     * GET /api/cart/user/{userId}/items/{productId}
     */
    @Operation(summary = "Get specific cart item", description = "Retrieves a specific item from the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cart item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}/items/{productId}")
    public ResponseEntity<CartItemResponse> getCartItem(
            @RequestAttribute("userId") Integer authenticatedUserId,
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId) {

        CartItem cartItem = cartItemService.getCartItem(authenticatedUserId, productId);
        CartItemResponse response = CartItemMapper.toCartItemResponse(cartItem);

        return ResponseEntity.ok(response);
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/user/{userId}/items/{productId}
     */
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a specific item in the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cart item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/user/{userId}/items/{productId}")
    public ResponseEntity<CartItemResponse> updateCartItemQuantity(
            @RequestAttribute("userId") Integer authenticatedUserId,
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId,
            @Valid @RequestBody UpdateCartItemDTO updateDTO) {

        CartItem updatedItem = cartItemService.updateQuantity(authenticatedUserId, productId, updateDTO.quantity());
        CartItemResponse response = CartItemMapper.toCartItemResponse(updatedItem);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/user/{userId}/items/{productId}
     */
    @Operation(summary = "Remove item from cart", description = "Removes a specific item from the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "404", description = "Cart item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/user/{userId}/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @RequestAttribute("userId") Integer authenticatedUserId,
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId) {

        cartItemService.removeFromCart(authenticatedUserId, productId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Clear user's cart
     * DELETE /api/cart/user/{userId}
     */
    @Operation(summary = "Clear cart", description = "Removes all items from the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        cartItemService.clearCart(authenticatedUserId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count
     * GET /api/cart/user/{userId}/count
     */
    @Operation(summary = "Get cart item count", description = "Returns the number of items in the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Integer> getCartItemCount(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        int count = cartItemService.getCartItemCount(authenticatedUserId);

        return ResponseEntity.ok(count);
    }

    /**
     * Get cart total
     * GET /api/cart/user/{userId}/total
     */
    @Operation(summary = "Get cart total", description = "Returns the total value of the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<BigDecimal> getCartTotal(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        BigDecimal total = cartItemService.getCartTotal(authenticatedUserId);

        return ResponseEntity.ok(total);
    }
}
