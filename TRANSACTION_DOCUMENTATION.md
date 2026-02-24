# Transaction Handling and Rollback Strategies

## Table of Contents
1. [Overview](#overview)
2. [Transaction Fundamentals](#transaction-fundamentals)
3. [Implementation Patterns](#implementation-patterns)
4. [Rollback Strategies](#rollback-strategies)
5. [Isolation Levels](#isolation-levels)
6. [Best Practices](#best-practices)
7. [Common Scenarios](#common-scenarios)
8. [Troubleshooting](#troubleshooting)

---

## Overview

SmartCommerce uses **Spring's declarative transaction management** with the `@Transactional` annotation to ensure data consistency and integrity across database operations.

### Why Transactions Matter

Transactions ensure **ACID** properties:
- **Atomicity**: All operations succeed or all fail (no partial updates)
- **Consistency**: Database remains in valid state
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Committed changes are permanent

### Key Features

- ✅ Declarative transaction management with `@Transactional`
- ✅ Automatic rollback on runtime exceptions
- ✅ Multiple isolation levels for concurrency control
- ✅ Propagation behavior for nested transactions
- ✅ Read-only optimization for query operations
- ✅ Custom rollback rules for checked exceptions

---

## Transaction Fundamentals

### 1. Basic Transaction Annotation

```java
@Service
@Transactional  // Class-level default settings
public class OrderServiceImp implements OrderService {
    
    @Transactional(readOnly = false)  // Method-level override
    public Order createOrder(Order order, List<OrderItem> orderItems) {
        // All database operations here are in ONE transaction
        // If any operation fails, ALL changes are rolled back
    }
}
```

### 2. Transaction Lifecycle

```
START TRANSACTION
    ↓
┌─────────────────────────────────┐
│  Execute Database Operations    │
│  - INSERT, UPDATE, DELETE       │
│  - SELECT (acquire locks)       │
└─────────────┬───────────────────┘
              │
         SUCCESS? 
         /      \
       YES      NO
        ↓        ↓
    COMMIT   ROLLBACK
        ↓        ↓
   Changes    Changes
   Saved     Discarded
```

### 3. Key Attributes

| Attribute | Values | Purpose |
|-----------|--------|---------|
| `readOnly` | true/false | Optimize read-only operations |
| `propagation` | REQUIRED, REQUIRES_NEW, etc. | Transaction nesting behavior |
| `isolation` | READ_COMMITTED, SERIALIZABLE, etc. | Concurrency control |
| `timeout` | seconds | Maximum transaction duration |
| `rollbackFor` | Exception classes | Custom rollback rules |
| `noRollbackFor` | Exception classes | Prevent rollback for specific exceptions |

---

## Implementation Patterns

### 1. Service-Level Transactions

**Pattern:** Apply `@Transactional` at the service layer, not repository or controller.

#### ✅ CORRECT Implementation

```java
@Service
@Transactional  // Default for all methods
public class OrderServiceImp implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryServiceInterface inventoryService;
    
    @Transactional(readOnly = false, 
                   propagation = Propagation.REQUIRED, 
                   isolation = Isolation.READ_COMMITTED)
    public Order createOrder(Order order, List<OrderItem> orderItems) {
        // Step 1: Validate and save order
        Order savedOrder = orderRepository.save(order);
        
        // Step 2: Create order items
        for (OrderItem item : orderItems) {
            orderItemRepository.save(item);
            
            // Step 3: Update inventory
            inventoryService.reduceStock(item.getProductId(), item.getQuantity());
        }
        
        // All steps succeed or ALL rollback
        return savedOrder;
    }
}
```

**Why Service Layer?**
- ✅ Encapsulates business logic
- ✅ Coordinates multiple repositories
- ✅ Manages transaction boundaries
- ✅ Handles validation and error handling

#### ❌ INCORRECT Implementation

```java
@RestController
@Transactional  // DON'T do this!
public class OrderController {
    // Controllers should NOT manage transactions
}

@Repository
@Transactional  // Already transactional by default
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Repositories get transactions from service layer
}
```

---

### 2. Read-Only Transactions

**Purpose:** Optimize queries that don't modify data.

```java
@Service
@Transactional
public class OrderServiceImp {
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllWithItemsOrderByDateDesc();
    }
    
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdWithItemsOrderByDateDesc(userId);
    }
}
```

**Benefits of `readOnly = true`:**
- ✅ Hibernate flush mode set to MANUAL (no dirty checking)
- ✅ Database can optimize read-only transactions
- ✅ Prevents accidental data modifications
- ✅ Better performance on large datasets

**Performance Impact:**
- Read-only: ~20-30% faster for complex queries
- Saves memory by skipping change detection

---

### 3. Write Transactions

**Purpose:** Ensure data integrity for create/update/delete operations.

```java
@Service
@Transactional
public class OrderServiceImp {
    
    @Transactional(readOnly = false)  // Explicit write transaction
    public Order updateOrderStatus(Long orderId, String newStatus) {
        // Validate status
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException("Invalid status: " + newStatus);
        }
        
        // Fetch order
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        // Business logic validation
        if ("cancelled".equals(newStatus) && "delivered".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel delivered order");
        }
        
        // Update status
        order.setStatus(newStatus);
        
        // If cancelled, restore inventory
        if ("cancelled".equals(newStatus)) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.restoreStock(item.getProductId(), item.getQuantity());
            }
        }
        
        return orderRepository.save(order);
        // COMMIT: All changes saved
        // ROLLBACK: If any exception, all changes reverted
    }
}
```

---

## Rollback Strategies

### 1. Automatic Rollback (Runtime Exceptions)

**Default Behavior:** Transaction automatically rolls back on **unchecked exceptions** (RuntimeException and subclasses).

```java
@Transactional
public Order createOrder(Order order, List<OrderItem> orderItems) {
    Order savedOrder = orderRepository.save(order);
    
    // Automatic rollback triggers
    if (orderItems.isEmpty()) {
        throw new BusinessException("Order must have items");
        // Transaction ROLLS BACK automatically
        // savedOrder is NOT persisted
    }
    
    return savedOrder;
}
```

**Common Runtime Exceptions:**
- `BusinessException` (custom)
- `ResourceNotFoundException` (custom)
- `IllegalArgumentException`
- `NullPointerException`
- `DataIntegrityViolationException`

### 2. Checked Exception Handling

**Default Behavior:** Checked exceptions do NOT trigger rollback by default.

#### Problem

```java
@Transactional
public void processOrder(Order order) throws IOException {
    orderRepository.save(order);
    
    // Checked exception - NO automatic rollback!
    sendConfirmationEmail(order);  // throws IOException
    
    // If email fails, order is STILL saved (probably not what you want)
}
```

#### Solution 1: Use `rollbackFor`

```java
@Transactional(rollbackFor = {IOException.class, Exception.class})
public void processOrder(Order order) throws IOException {
    orderRepository.save(order);
    sendConfirmationEmail(order);  // Now triggers rollback
}
```

#### Solution 2: Convert to Runtime Exception

```java
@Transactional
public void processOrder(Order order) {
    orderRepository.save(order);
    
    try {
        sendConfirmationEmail(order);
    } catch (IOException e) {
        throw new BusinessException("Email failed", e);  // Triggers rollback
    }
}
```

### 3. Partial Rollback Prevention

**Problem:** Prevent some exceptions from triggering rollback.

```java
@Transactional(noRollbackFor = OptimisticLockException.class)
public Order updateOrder(Order order) {
    // If optimistic lock fails, don't rollback other operations
    return orderRepository.save(order);
}
```

### 4. Manual Rollback

```java
@Transactional
public void complexOperation() {
    try {
        // Step 1: Save order
        Order order = orderRepository.save(new Order());
        
        // Step 2: External API call
        boolean apiSuccess = callExternalPaymentAPI(order);
        
        if (!apiSuccess) {
            // Force rollback even without exception
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return;
        }
        
        // Step 3: Complete order
        order.setStatus("confirmed");
        orderRepository.save(order);
        
    } catch (Exception e) {
        // Automatic rollback
        throw new BusinessException("Operation failed", e);
    }
}
```

---

## Isolation Levels

Isolation levels control how concurrent transactions interact.

### 1. READ_COMMITTED (Default - Recommended)

**Used in:** OrderServiceImp.createOrder()

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public Order createOrder(Order order, List<OrderItem> orderItems) {
    // Transaction only sees committed data from other transactions
}
```

**Characteristics:**
- ✅ Prevents dirty reads (reading uncommitted changes)
- ✅ Good balance of consistency and performance
- ❌ Allows non-repeatable reads
- ❌ Allows phantom reads

**Use When:**
- Default choice for most operations
- Need good performance with reasonable consistency
- Order processing, user registration, etc.

### 2. REPEATABLE_READ

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public BigDecimal calculateOrderTotal(Long orderId) {
    // Same query returns same results within transaction
    Order order = orderRepository.findById(orderId).get();
    BigDecimal total = calculateTotal(order);
    
    // Even if another transaction updates order, we see original data
    return total;
}
```

**Characteristics:**
- ✅ Prevents dirty reads
- ✅ Prevents non-repeatable reads (same query = same result)
- ❌ Allows phantom reads
- ❌ More locking, slower performance

**Use When:**
- Need consistent data throughout transaction
- Financial calculations
- Report generation

### 3. SERIALIZABLE

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void transferInventory(Long fromProduct, Long toProduct, int quantity) {
    // Transactions execute as if serial (one at a time)
    // Prevents all concurrency issues
}
```

**Characteristics:**
- ✅ Prevents all concurrency issues (dirty, non-repeatable, phantom reads)
- ❌ Slowest performance (high locking)
- ❌ Can cause deadlocks

**Use When:**
- Absolute consistency required
- Financial transfers
- Inventory critical operations
- Use sparingly!

### 4. READ_UNCOMMITTED

```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public List<Order> getRecentOrders() {
    // Reads uncommitted changes (dirty reads allowed)
    // Fastest but least consistent
}
```

**Characteristics:**
- ✅ Fastest performance
- ❌ Allows dirty reads (very risky!)
- ❌ Not recommended for production

**Use When:**
- Rarely! Only for non-critical reporting
- Temporary data
- Analytics where exactness not critical

### Isolation Level Comparison

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance |
|-------|------------|---------------------|--------------|-------------|
| READ_UNCOMMITTED | ❌ Allowed | ❌ Allowed | ❌ Allowed | ⚡⚡⚡⚡ |
| READ_COMMITTED | ✅ Prevented | ❌ Allowed | ❌ Allowed | ⚡⚡⚡ |
| REPEATABLE_READ | ✅ Prevented | ✅ Prevented | ❌ Allowed | ⚡⚡ |
| SERIALIZABLE | ✅ Prevented | ✅ Prevented | ✅ Prevented | ⚡ |

---

## Propagation Behavior

Controls how transactions behave when calling other transactional methods.

### 1. REQUIRED (Default)

```java
@Transactional(propagation = Propagation.REQUIRED)
public Order createOrder(Order order, List<OrderItem> orderItems) {
    // If transaction exists, use it
    // If no transaction, create new one
    
    saveOrderItems(orderItems);  // Uses same transaction
}

@Transactional(propagation = Propagation.REQUIRED)
public void saveOrderItems(List<OrderItem> items) {
    // Joins parent transaction if exists
}
```

**Result:** Both methods share ONE transaction.

### 2. REQUIRES_NEW

```java
@Transactional(propagation = Propagation.REQUIRED)
public Order createOrder(Order order) {
    Order savedOrder = orderRepository.save(order);
    
    // Audit log gets its own transaction
    auditService.logOrderCreation(savedOrder);
    
    return savedOrder;
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logOrderCreation(Order order) {
    // NEW transaction (independent of parent)
    auditRepository.save(new AuditLog(order));
    // Commits IMMEDIATELY, even if parent rolls back
}
```

**Use Case:** Audit logging that should persist even if main operation fails.

### 3. NESTED

```java
@Transactional
public void processOrders(List<Order> orders) {
    for (Order order : orders) {
        try {
            processOrder(order);  // Nested transaction
        } catch (Exception e) {
            // Individual order failure doesn't affect others
            log.error("Order {} failed", order.getId());
        }
    }
}

@Transactional(propagation = Propagation.NESTED)
public void processOrder(Order order) {
    // Nested transaction (savepoint)
    // Can rollback independently
}
```

**Result:** Parent can continue even if nested transaction rolls back.

---

## Best Practices

### ✅ DO

#### 1. Keep Transactions Short

```java
// GOOD - Quick transaction
@Transactional
public Order createOrder(Order order) {
    validateOrder(order);           // No DB access
    return orderRepository.save(order);  // Quick save
}

// GOOD - Separate transaction
@Transactional
public void sendConfirmation(Order order) {
    emailService.send(order);  // Separate transaction
}
```

```java
// BAD - Long transaction
@Transactional
public Order createOrderAndNotify(Order order) {
    Order saved = orderRepository.save(order);
    Thread.sleep(5000);  // DON'T hold transaction!
    emailService.send(saved);  // External call in transaction!
    return saved;
}
```

#### 2. Use Read-Only for Queries

```java
@Transactional(readOnly = true)
public List<Product> getProducts() {
    return productRepository.findAll();
}
```

#### 3. Handle Exceptions Properly

```java
@Transactional
public Order createOrder(Order order) {
    try {
        return orderRepository.save(order);
    } catch (DataIntegrityViolationException e) {
        throw new BusinessException("Duplicate order", e);
        // Triggers rollback
    }
}
```

#### 4. Set Appropriate Isolation Level

```java
// Financial operations - REPEATABLE_READ
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void processPayment(Payment payment) { }

// Regular operations - READ_COMMITTED
@Transactional(isolation = Isolation.READ_COMMITTED)
public Order createOrder(Order order) { }
```

#### 5. Use Propagation Wisely

```java
// Independent audit log
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAccess(User user) {
    auditRepository.save(new AuditLog(user));
}
```

---

### ❌ DON'T

#### 1. Don't Use Transactions in Controllers

```java
// BAD
@RestController
@Transactional
public class OrderController { }

// GOOD
@RestController
public class OrderController {
    private final OrderService orderService;  // Service has @Transactional
}
```

#### 2. Don't Catch and Swallow Exceptions

```java
// BAD - Transaction won't rollback!
@Transactional
public Order createOrder(Order order) {
    try {
        return orderRepository.save(order);
    } catch (Exception e) {
        e.printStackTrace();  // Silent failure!
        return null;  // Transaction commits!
    }
}

// GOOD
@Transactional
public Order createOrder(Order order) {
    try {
        return orderRepository.save(order);
    } catch (Exception e) {
        log.error("Order creation failed", e);
        throw new BusinessException("Order failed", e);  // Rollback
    }
}
```

#### 3. Don't Make External Calls Inside Transactions

```java
// BAD - Holding database locks during external call
@Transactional
public Order createOrder(Order order) {
    Order saved = orderRepository.save(order);
    externalPaymentAPI.charge(saved);  // DON'T!
    return saved;
}

// GOOD - Separate transactions
@Transactional
public Order createOrder(Order order) {
    return orderRepository.save(order);
}

public void processPayment(Order order) {
    // External call outside transaction
    externalPaymentAPI.charge(order);
}
```

#### 4. Don't Use Wrong Isolation Level

```java
// BAD - Overkill for simple query
@Transactional(isolation = Isolation.SERIALIZABLE)
public List<Product> getProducts() {
    return productRepository.findAll();
}

// GOOD - Use default
@Transactional(readOnly = true)
public List<Product> getProducts() {
    return productRepository.findAll();
}
```

---

## Common Scenarios

### Scenario 1: Order Creation (Multi-Step Transaction)

```java
@Transactional(
    readOnly = false,
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED
)
public Order createOrder(Order order, List<OrderItem> orderItems) {
    // Transaction START
    
    // Step 1: Validate user exists
    User user = userRepository.findById(order.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", order.getUserId()));
    
    // Step 2: Validate order items
    if (orderItems == null || orderItems.isEmpty()) {
        throw new BusinessException("Order must contain items");
        // ROLLBACK - No order created
    }
    
    // Step 3: Calculate and set total
    BigDecimal total = orderItems.stream()
        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.setTotalAmount(total);
    
    // Step 4: Save order
    Order savedOrder = orderRepository.save(order);
    
    // Step 5: Save order items and update inventory
    for (OrderItem item : orderItems) {
        item.setOrderId(savedOrder.getOrderId());
        orderItemRepository.save(item);
        
        // Reduce inventory (throws exception if insufficient stock)
        inventoryService.reduceStock(item.getProductId(), item.getQuantity());
        // If fails here, EVERYTHING rolls back (order, items, previous inventory updates)
    }
    
    // Step 6: Clear user's cart
    cartItemService.clearCart(order.getUserId());
    
    // Transaction COMMIT - All changes saved atomically
    return savedOrder;
}
```

**Rollback Scenarios:**
- User doesn't exist → ROLLBACK before any save
- No order items → ROLLBACK before any save
- Insufficient inventory → ROLLBACK all (order + items + inventory updates)
- Cart clear fails → ROLLBACK everything

---

### Scenario 2: Order Cancellation (Compensating Transaction)

```java
@Transactional(readOnly = false)
public Order cancelOrder(Long orderId) {
    // Transaction START
    
    // Step 1: Load order
    Order order = orderRepository.findByIdWithItems(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    
    // Step 2: Validate can cancel
    if ("delivered".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
        throw new BusinessException("Cannot cancel order in status: " + order.getStatus());
        // ROLLBACK - No changes
    }
    
    // Step 3: Restore inventory for all items
    for (OrderItem item : order.getOrderItems()) {
        inventoryService.restoreStock(item.getProductId(), item.getQuantity());
        // If any restore fails, ALL previous restores rolled back
    }
    
    // Step 4: Update order status
    order.setStatus("cancelled");
    Order updated = orderRepository.save(order);
    
    // Transaction COMMIT - Status updated AND inventory restored
    return updated;
}
```

**Atomicity Guarantee:**
- Either inventory fully restored AND status updated
- OR no changes at all

---

### Scenario 3: Concurrent Order Processing

```java
@Transactional(
    isolation = Isolation.REPEATABLE_READ,  // Prevent stock changes during transaction
    propagation = Propagation.REQUIRED
)
public Order createOrder(Order order, List<OrderItem> orderItems) {
    // Multiple users ordering same product concurrently
    
    for (OrderItem item : orderItems) {
        // Check stock
        int availableStock = inventoryService.getStock(item.getProductId());
        
        if (availableStock < item.getQuantity()) {
            throw new BusinessException("Insufficient stock");
            // ROLLBACK
        }
        
        // Reduce stock (with locking)
        inventoryService.reduceStock(item.getProductId(), item.getQuantity());
        // Other concurrent transactions wait here
    }
    
    // Save order
    return orderRepository.save(order);
    // COMMIT - Releases locks
}
```

**Concurrency Control:**
- REPEATABLE_READ ensures stock doesn't change mid-transaction
- Pessimistic locking prevents overselling
- Other orders wait for lock release

---

## Troubleshooting

### Problem 1: Transaction Not Rolling Back

**Symptom:** Data saved despite exception

**Causes:**
1. Exception caught and swallowed
2. Checked exception without `rollbackFor`
3. Transaction not active (controller level)

**Solution:**
```java
// Ensure exception propagates
@Transactional
public void saveData() {
    try {
        repository.save(data);
    } catch (Exception e) {
        log.error("Failed", e);
        throw new BusinessException("Save failed", e);  // Re-throw!
    }
}

// Or use rollbackFor
@Transactional(rollbackFor = Exception.class)
public void saveData() throws Exception {
    repository.save(data);
}
```

---

### Problem 2: LazyInitializationException

**Symptom:** `org.hibernate.LazyInitializationException: could not initialize proxy`

**Cause:** Accessing lazy-loaded data outside transaction

**Solution:**
```java
// OPTION 1: Use JOIN FETCH
@Transactional(readOnly = true)
public Order getOrder(Long id) {
    return orderRepository.findByIdWithItems(id);  // Items loaded
}

// OPTION 2: Access data inside transaction
@Transactional(readOnly = true)
public OrderDTO getOrderWithItems(Long id) {
    Order order = orderRepository.findById(id).get();
    order.getOrderItems().size();  // Force load while in transaction
    return toDTO(order);
}
```

---

### Problem 3: Deadlocks

**Symptom:** `Deadlock found when trying to get lock`

**Cause:** Two transactions waiting for each other's locks

**Solution:**
```java
// Always lock in same order
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void processOrders(List<Long> orderIds) {
    // Sort IDs to ensure consistent lock order
    Collections.sort(orderIds);
    
    for (Long id : orderIds) {
        Order order = orderRepository.findById(id).get();
        // Process...
    }
}
```

---

### Problem 4: Transaction Timeout

**Symptom:** `Transaction timeout`

**Cause:** Long-running transaction

**Solution:**
```java
// Set timeout
@Transactional(timeout = 30)  // 30 seconds
public void longOperation() {
    // Process...
}

// Or break into smaller transactions
@Transactional
public void processBatch(List<Item> items) {
    for (Item item : items) {
        processItem(item);  // Each item = separate transaction
    }
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processItem(Item item) {
    // Quick transaction
}
```

---

## Summary

### Transaction Strategy Overview

| Operation Type | readOnly | Isolation | Propagation |
|----------------|----------|-----------|-------------|
| Simple Query | true | READ_COMMITTED | REQUIRED |
| Complex Query | true | READ_COMMITTED | REQUIRED |
| Create/Update | false | READ_COMMITTED | REQUIRED |
| Delete | false | READ_COMMITTED | REQUIRED |
| Financial | false | REPEATABLE_READ | REQUIRED |
| Audit Log | false | READ_COMMITTED | REQUIRES_NEW |

### Rollback Rules

- ✅ **Automatic Rollback:** RuntimeException and subclasses
- ❌ **No Rollback:** Checked exceptions (unless configured)
- 🔧 **Custom:** Use `rollbackFor` and `noRollbackFor`

### Key Takeaways

1. **Service Layer** - Always manage transactions in services, not controllers or repositories
2. **Use `readOnly = true`** - For all queries (20-30% performance gain)
3. **Default Isolation** - READ_COMMITTED is best for 90% of cases
4. **Keep Transactions Short** - No external API calls, file I/O, or long computations
5. **Let Exceptions Propagate** - Don't catch and swallow, transaction won't rollback
6. **Use JOIN FETCH** - Prevent LazyInitializationException
7. **Test Rollback** - Verify rollback behavior in unit tests

### References

- Spring Transaction Documentation: https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction
- ACID Properties: https://en.wikipedia.org/wiki/ACID
- Isolation Levels: https://en.wikipedia.org/wiki/Isolation_(database_systems)
