-- ============================================================
-- INDEX VALIDATION SCRIPT - User Story 3.2
-- Purpose: Validate index usage for frequently accessed columns
-- ============================================================

USE ecommerce_db;

-- ============================================================
-- EXPLAIN ANALYZE - Before Optimization Baseline
-- ============================================================

-- Test 1: Order history query (user orders with items)
EXPLAIN SELECT o.*, oi.* 
FROM orders o 
LEFT JOIN orderitems oi ON o.order_id = oi.order_id 
WHERE o.user_id = 2 
ORDER BY o.order_date DESC;

-- Test 2: Order status reporting
EXPLAIN SELECT o.*, oi.* 
FROM orders o 
LEFT JOIN orderitems oi ON o.order_id = oi.order_id 
WHERE o.status = 'pending' 
ORDER BY o.order_date DESC;

-- Test 3: Date range reporting query
EXPLAIN SELECT o.*, oi.* 
FROM orders o 
LEFT JOIN orderitems oi ON o.order_id = oi.order_id 
WHERE o.order_date BETWEEN '2026-01-01' AND '2026-12-31' 
ORDER BY o.order_date DESC;

-- Test 4: Product search with category filter
EXPLAIN SELECT * 
FROM products 
WHERE category_id = 1 
  AND name LIKE '%laptop%' 
ORDER BY price;

-- Test 5: Cart total calculation
EXPLAIN SELECT SUM(ci.quantity * p.price) 
FROM cartitems ci 
JOIN products p ON ci.product_id = p.product_id 
WHERE ci.user_id = 2;

-- ============================================================
-- INDEX CARDINALITY CHECK
-- ============================================================

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ecommerce_db'
  AND TABLE_NAME IN ('Orders', 'OrderItems', 'Products', 'CartItems')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================
-- QUERY PERFORMANCE TESTING - Record Execution Times
-- ============================================================

-- Enable query profiling
SET profiling = 1;

-- Test Query 1: User order history (BEFORE optimization marker)
SELECT o.order_id, o.order_date, o.status, o.total_amount
FROM orders o
WHERE o.user_id = 2
ORDER BY o.order_date DESC;

-- Test Query 2: Order details with items  
SELECT o.*, oi.*
FROM orders o
LEFT JOIN orderitems oi ON o.order_id = oi.order_id
WHERE o.user_id = 2
ORDER BY o.order_date DESC;

-- Test Query 3: Status reporting
SELECT o.status, COUNT(*) as order_count, SUM(o.total_amount) as total_revenue
FROM orders o
WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY o.status;

-- Test Query 4: Product performance report
SELECT p.product_id, p.name, COUNT(oi.order_item_id) as times_ordered, 
       SUM(oi.quantity) as total_quantity
FROM products p
LEFT JOIN orderitems oi ON p.product_id = oi.product_id
GROUP BY p.product_id, p.name
ORDER BY times_ordered DESC
LIMIT 10;

-- Show profiling results
SHOW PROFILES;

-- Detailed profile for last query
SHOW PROFILE FOR QUERY 4;

-- Disable profiling after tests
SET profiling = 0;

-- ============================================================
-- INDEX USAGE VALIDATION
-- ============================================================

-- Check if indexes are being used (after running queries)
SELECT 
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_STAR as 'Times Used',
    COUNT_READ as 'Rows Read',
    COUNT_FETCH as 'Rows Fetched'
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'ecommerce_db'
  AND INDEX_NAME IS NOT NULL
ORDER BY COUNT_STAR DESC;

-- ============================================================
-- RECOMMENDATIONS
-- ============================================================

-- Check for missing or unused indexes
SELECT 
    t.TABLE_NAME,
    t.TABLE_ROWS,
    ROUND(((t.DATA_LENGTH + t.INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size (MB)',
    COUNT(DISTINCT s.INDEX_NAME) as 'Index Count'
FROM information_schema.TABLES t
LEFT JOIN information_schema.STATISTICS s ON t.TABLE_NAME = s.TABLE_NAME
WHERE t.TABLE_SCHEMA = 'ecommerce_db'
  AND t.TABLE_TYPE = 'BASE TABLE'
GROUP BY t.TABLE_NAME, t.TABLE_ROWS, t.DATA_LENGTH, t.INDEX_LENGTH
ORDER BY t.TABLE_ROWS DESC;
