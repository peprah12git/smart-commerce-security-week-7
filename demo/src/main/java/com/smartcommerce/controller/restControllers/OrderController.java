package com.smartcommerce.controller.restControllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.CreateOrderDTO;
import com.smartcommerce.dtos.request.OrderItemDTO;
import com.smartcommerce.dtos.request.UpdateOrderStatusDTO;
import com.smartcommerce.dtos.response.OrderItemResponse;
import com.smartcommerce.dtos.response.OrderResponse;
import com.smartcommerce.dtos.response.PagedResponse;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.Order;
import com.smartcommerce.model.OrderItem;
import com.smartcommerce.service.serviceInterface.OrderService;
import com.smartcommerce.utils.OrderMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Order management
 * Handles HTTP requests for order CRUD operations
 * Base URL: /api/orders
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management API — create, view, update status, and cancel orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order
     * POST /api/orders
     */
    @Operation(summary = "Create a new order", description = "Creates a new order with the specified items for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderDTO createOrderDTO,
            @RequestAttribute("userId") Integer userId) {

        // Create Order object
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("pending");

        // Convert OrderItemDTOs to OrderItem entities
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : createOrderDTO.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.productId());
            item.setQuantity(itemDTO.quantity());
            if (itemDTO.unitPrice() != null) {
                item.setUnitPrice(itemDTO.unitPrice());
            }
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);

        Order createdOrder = orderService.createOrder(order, orderItems);
        OrderResponse response = OrderMapper.toOrderResponse(createdOrder);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get all orders
     * GET /api/orders
     */
    @Operation(summary = "Get all orders", description = "Retrieves all orders in the system")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponse> response = OrderMapper.toOrderResponseList(orders);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all orders with pagination
     * GET /api/orders/paged?page=0&size=10&sort=orderDate,desc
     */
    @Operation(summary = "Get all orders (paginated)", description = "Retrieves all orders with pagination. Default page size is 10, sorted by order date descending.")
    @ApiResponse(responseCode = "200", description = "Paginated orders retrieved successfully")
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrdersPaged(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<Order> ordersPage = orderService.getAllOrders(pageable);
        Page<OrderResponse> responsePage = ordersPage.map(OrderMapper::toOrderResponse);
        PagedResponse<OrderResponse> pagedResponse = PagedResponse.of(responsePage);

        return ResponseEntity.ok(pagedResponse);
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by its ID including all order items")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true, example = "1")
            @PathVariable int orderId) {

        Order order = orderService.getOrderById(orderId);
        OrderResponse response = OrderMapper.toOrderResponse(order);
        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by user ID
     * GET /api/orders/user/{userId}
     */
    @Operation(summary = "Get orders by user", description = "Retrieves all orders for a specific user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        List<Order> orders = orderService.getOrdersByUserId(authenticatedUserId);
        List<OrderResponse> response = OrderMapper.toOrderResponseList(orders);
        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by user ID with pagination
     * GET /api/orders/user/{userId}/paged?page=0&size=10&sort=orderDate,desc
     */
    @Operation(summary = "Get orders by user (paginated)", description = "Retrieves paginated orders for a specific user. Default page size is 10, sorted by order date descending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<PagedResponse<OrderResponse>> getOrdersByUserIdPaged(
            @RequestAttribute("userId") Integer authenticatedUserId,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Order> ordersPage = orderService.getOrdersByUserId(authenticatedUserId, pageable);
        Page<OrderResponse> responsePage = ordersPage.map(OrderMapper::toOrderResponse);
        PagedResponse<OrderResponse> pagedResponse = PagedResponse.of(responsePage);

        return ResponseEntity.ok(pagedResponse);
    }

    /**
     * Update order status
     * PATCH /api/orders/{orderId}/status
     */
    @Operation(summary = "Update order status", description = "Updates the status of an existing order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid status transition",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "1")
            @PathVariable int orderId,
            @Valid @RequestBody UpdateOrderStatusDTO updateStatusDTO) {

        Order updatedOrder = orderService.updateOrderStatus(orderId, updateStatusDTO.status());
        OrderResponse response = OrderMapper.toOrderResponse(updatedOrder);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order
     * POST /api/orders/{orderId}/cancel
     */
    @Operation(summary = "Cancel an order", description = "Cancels an existing order (sets status to 'cancelled')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true, example = "1")
            @PathVariable int orderId) {

        Order cancelledOrder = orderService.cancelOrder(orderId);
        OrderResponse response = OrderMapper.toOrderResponse(cancelledOrder);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an order
     * DELETE /api/orders/{orderId}
     */
    @Operation(summary = "Delete an order", description = "Permanently deletes an order and all its items")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Order ID", required = true, example = "1")
            @PathVariable int orderId) {

        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get order items for an order
     * GET /api/orders/{orderId}/items
     */
    @Operation(summary = "Get order items", description = "Retrieves all items for a specific order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order items retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(
            @Parameter(description = "Order ID", required = true, example = "1")
            @PathVariable int orderId) {

        List<OrderItem> items = orderService.getOrderItems(orderId);
        List<OrderItemResponse> response = OrderMapper.toOrderItemResponseList(items);
        return ResponseEntity.ok(response);
    }

    /**
     * Checkout from cart
     * POST /api/orders/checkout/{userId}
     */
    @Operation(summary = "Checkout from cart", description = "Creates an order from user's cart items, validates stock, deducts inventory, and clears cart")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully from cart",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cart is empty or insufficient stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/checkout/{userId}")
    public ResponseEntity<OrderResponse> checkoutFromCart(
            @RequestAttribute("userId") Integer authenticatedUserId) {

        Order order = orderService.checkoutFromCart(authenticatedUserId);
        OrderResponse response = OrderMapper.toOrderResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
