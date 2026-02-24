-- ============================================================
-- QUERY OPTIMIZATION SCRIPT - User Story 3.2
-- Purpose: Add composite indexes for complex query optimization
-- Target: Order history and reporting queries
-- ============================================================

USE ecommerce_db;

-- ============================================================
-- COMPOSITE INDEXES FOR ORDER QUERIES
-- ============================================================

-- Composite index for user orders sorted by date (order history queries)
CREATE INDEX IF NOT EXISTS idx_orders_user_date 
ON Orders(user_id, order_date DESC);

-- Composite index for user orders filtered by status
CREATE INDEX IF NOT EXISTS idx_orders_user_status 
ON Orders(user_id, status);

-- Composite index for order reporting queries (status + date range)
CREATE INDEX IF NOT EXISTS idx_orders_status_date 
ON Orders(status, order_date DESC);

-- ============================================================
-- COMPOSITE INDEXES FOR ORDER ITEMS
-- ============================================================

-- Composite index for order items with product details (reporting)
CREATE INDEX IF NOT EXISTS idx_order_items_product_order 
ON OrderItems(product_id, order_id);

-- ============================================================
-- COMPOSITE INDEXES FOR PRODUCT QUERIES
-- ============================================================

-- Composite index for product search and filtering
CREATE INDEX IF NOT EXISTS idx_products_category_name 
ON Products(category_id, name);

-- Index for price range queries
CREATE INDEX IF NOT EXISTS idx_products_price 
ON Products(price);

-- ============================================================
-- COMPOSITE INDEXES FOR CART QUERIES
-- ============================================================

-- Composite index for cart total calculations
CREATE INDEX IF NOT EXISTS idx_cart_user_product 
ON CartItems(user_id, product_id);

-- ============================================================
-- VERIFY INDEXES
-- ============================================================

-- Show all indexes on Orders table
SHOW INDEX FROM Orders;

-- Show all indexes on OrderItems table
SHOW INDEX FROM OrderItems;

-- Show all indexes on Products table
SHOW INDEX FROM Products;

-- Show all indexes on CartItems table
SHOW INDEX FROM CartItems;

-- ============================================================
-- INDEX USAGE STATISTICS (Optional - for monitoring)
-- ============================================================

SELECT 
    'Orders' as table_name,
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    NULLABLE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ecommerce_db' 
  AND TABLE_NAME = 'Orders'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

SELECT 
    'OrderItems' as table_name,
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    NULLABLE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ecommerce_db' 
  AND TABLE_NAME = 'OrderItems'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
