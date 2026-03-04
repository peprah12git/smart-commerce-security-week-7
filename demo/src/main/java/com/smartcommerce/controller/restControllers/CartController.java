package com.smartcommerce.controller.restControllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.smartcommerce.security.SecurityUtils;
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
 * REST Controller for Shopping Cart management.
 * All endpoints operate on the authenticated user's cart — no userId in the URL.
 * Base URL: /api/carts
 */
@RestController
@RequestMapping("/api/carts")
@Tag(name = "Carts", description = "Shopping cart management API — add, update, remove items and view cart")
@PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF')")
public class CartController {

    private final CartItemService cartItemService;
    private final SecurityUtils securityUtils;

    public CartController(CartItemService cartItemService, SecurityUtils securityUtils) {
        this.cartItemService = cartItemService;
        this.securityUtils = securityUtils;
    }

    // POST /api/carts/items
    @Operation(summary = "Add item to cart",
            description = "Adds a product to the authenticated user's cart, or updates quantity if already present")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(@Valid @RequestBody AddToCartDTO dto) {
        int userId = securityUtils.getCurrentUserId();
        CartItem cartItem = cartItemService.addToCart(userId, dto.productId(), dto.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CartItemMapper.toCartItemResponse(cartItem));
    }

    // GET /api/carts/me
    @Operation(summary = "Get my cart",
            description = "Returns the authenticated user's complete cart with all items, item count, and total")
    @ApiResponse(responseCode = "200", description = "Cart retrieved",
            content = @Content(schema = @Schema(implementation = CartResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart() {
        int userId = securityUtils.getCurrentUserId();
        List<CartItem> items = cartItemService.getCartItemsWithDetails(userId);
        List<CartItemResponse> itemResponses = CartItemMapper.toCartItemResponseList(items);
        int count = cartItemService.getCartItemCount(userId);
        BigDecimal total = cartItemService.getCartTotal(userId);
        return ResponseEntity.ok(new CartResponse(userId, itemResponses, count, total));
    }

    // GET /api/carts/me/items
    @Operation(summary = "Get my cart items",
            description = "Returns all items in the authenticated user's cart with product details")
    @ApiResponse(responseCode = "200", description = "Items retrieved")
    @GetMapping("/me/items")
    public ResponseEntity<List<CartItemResponse>> getMyCartItems() {
        int userId = securityUtils.getCurrentUserId();
        List<CartItem> items = cartItemService.getCartItemsWithDetails(userId);
        return ResponseEntity.ok(CartItemMapper.toCartItemResponseList(items));
    }

    // GET /api/carts/me/items/{productId}
    @Operation(summary = "Get specific cart item",
            description = "Retrieves a specific product from the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item retrieved",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me/items/{productId}")
    public ResponseEntity<CartItemResponse> getMyCartItem(
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId) {
        int userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(CartItemMapper.toCartItemResponse(cartItemService.getCartItem(userId, productId)));
    }

    // PUT /api/carts/me/items/{productId}
    @Operation(summary = "Update cart item quantity",
            description = "Updates the quantity of a specific item in the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me/items/{productId}")
    public ResponseEntity<CartItemResponse> updateCartItemQuantity(
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId,
            @Valid @RequestBody UpdateCartItemDTO updateDTO) {
        int userId = securityUtils.getCurrentUserId();
        CartItem updated = cartItemService.updateQuantity(userId, productId, updateDTO.quantity());
        return ResponseEntity.ok(CartItemMapper.toCartItemResponse(updated));
    }

    // DELETE /api/carts/me/items/{productId}
    @Operation(summary = "Remove item from cart",
            description = "Removes a specific item from the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/me/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @Parameter(description = "Product ID", required = true, example = "5")
            @PathVariable int productId) {
        int userId = securityUtils.getCurrentUserId();
        cartItemService.removeFromCart(userId, productId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/carts/me
    @Operation(summary = "Clear cart", description = "Removes all items from the authenticated user's cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared")
    @DeleteMapping("/me")
    public ResponseEntity<Void> clearCart() {
        int userId = securityUtils.getCurrentUserId();
        cartItemService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/carts/me/count
    @Operation(summary = "Get cart item count",
            description = "Returns the number of distinct items in the authenticated user's cart")
    @ApiResponse(responseCode = "200", description = "Count retrieved")
    @GetMapping("/me/count")
    public ResponseEntity<Integer> getCartItemCount() {
        int userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartItemService.getCartItemCount(userId));
    }

    // GET /api/carts/me/total
    @Operation(summary = "Get cart total",
            description = "Returns the total value of the authenticated user's cart")
    @ApiResponse(responseCode = "200", description = "Total retrieved")
    @GetMapping("/me/total")
    public ResponseEntity<BigDecimal> getCartTotal() {
        int userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartItemService.getCartTotal(userId));
    }
}
