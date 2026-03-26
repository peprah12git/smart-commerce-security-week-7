CREATE DATABASE ecommerce_db;
USE ecommerce_db;

CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(20) DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE Products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);

CREATE TABLE Inventory (
    inventory_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT UNIQUE,
    quantity_available INT NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
);

CREATE TABLE Orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50),
    total_amount DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE OrderItems (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    product_id INT,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
);

CREATE TABLE Reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    product_id INT,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
);

 CREATE TABLE CartItems (
     cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
     user_id INT NOT NULL,
     product_id INT NOT NULL,
     quantity INT NOT NULL DEFAULT 1,
     added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
     FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE,
     UNIQUE KEY unique_user_product (user_id, product_id)
 );

 CREATE INDEX idx_cart_user ON CartItems(user_id);
 CREATE INDEX idx_cart_product ON CartItems(product_id);

-- ============ INDEXES FOR PERFORMANCE ============
CREATE INDEX idx_users_email ON Users(email);
CREATE INDEX idx_products_category ON Products(category_id);
CREATE INDEX idx_products_name ON Products(name);
CREATE INDEX idx_products_price ON Products(price);
CREATE FULLTEXT INDEX idx_products_fulltext ON Products(name, description);  -- ✅ Full-text search
CREATE INDEX idx_inventory_product ON Inventory(product_id);
CREATE INDEX idx_orders_user ON Orders(user_id);
CREATE INDEX idx_orders_date ON Orders(order_date DESC);
CREATE INDEX idx_order_items_order ON OrderItems(order_id);
CREATE INDEX idx_order_items_product ON OrderItems(product_id);
CREATE INDEX idx_reviews_product ON Reviews(product_id);
CREATE INDEX idx_reviews_user ON Reviews(user_id);

-- ============ SAMPLE DATA ============

-- Insert Sample Users with plaintext passwords
-- Password: "password123"
INSERT INTO Users (name, email, password, phone, address, role) VALUES
('Admin User', 'admin@test.com', 'password123', '555-0001', '123 Admin St', 'admin'),
('John Doe', 'john.doe@email.com', 'password123', '555-1001', '123 Main St, City A', 'user'),
('Jane Smith', 'jane.smith@email.com', 'password123', '555-1002', '456 Oak Ave, City B', 'user'),
('Bob Johnson', 'bob.j@email.com', 'password123', '555-1003', '789 Pine Rd, City C', 'user'),
('Alice Williams', 'alice.w@email.com', 'password123', '555-1004', '321 Elm St, City D', 'user'),
('Charlie Brown', 'charlie.b@email.com', 'password123', '555-1005', '654 Maple Dr, City E', 'user');

-- Insert Sample Categories
INSERT INTO Categories (category_name, description) VALUES
('Electronics', 'Electronic devices and accessories'),
('Clothing', 'Apparel and fashion items'),
('Home & Garden', 'Home improvement and garden products'),
('Sports & Outdoors', 'Sports equipment and outdoor gear'),
('Books & Media', 'Books, movies, and digital media');

-- Insert Sample Products
INSERT INTO Products (name, description, price, category_id) VALUES
('Laptop Pro 15', 'High-performance laptop with Intel i7 processor', 1299.99, 1),
('Wireless Headphones', 'Noise-cancelling Bluetooth headphones', 199.99, 1),
('USB-C Cable 2M', 'Fast charging USB-C cable', 29.99, 1),
('Running Shoes', 'Professional grade running shoes', 149.99, 2),
('Winter Jacket', 'Warm waterproof winter jacket', 299.99, 2),
('Office Chair', 'Ergonomic office chair with lumbar support', 399.99, 3),
('LED Desk Lamp', 'Adjustable LED desk lamp with USB charging', 79.99, 3),
('Yoga Mat', 'Non-slip exercise yoga mat', 49.99, 4),
('Tennis Racket', 'Professional tennis racket', 189.99, 4),
('The Art of Programming', 'Comprehensive programming guide', 59.99, 5),
('Smart Watch', 'Fitness tracking smartwatch with heart rate monitor', 249.99, 1),
('Coffee Maker', 'Programmable coffee maker with timer', 89.99, 3);

-- Insert Sample Inventory
INSERT INTO Inventory (product_id, quantity_available) VALUES
(1, 15),
(2, 45),
(3, 200),
(4, 30),
(5, 25),
(6, 12),
(7, 50),
(8, 85),
(9, 20),
(10, 40),
(11, 35),
(12, 60);

-- Insert Sample Orders
INSERT INTO Orders (user_id, status, total_amount) VALUES
(1, 'completed', 1499.97),
(2, 'pending', 449.98),
(3, 'completed', 879.97),
(4, 'shipped', 299.99),
(1, 'completed', 189.99);

-- Insert Sample Order Items
INSERT INTO OrderItems (order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 1299.99),
(1, 2, 1, 199.99),
(2, 4, 3, 149.99),
(3, 11, 1, 249.99),
(3, 7, 1, 79.99),
(3, 10, 1, 149.99),
(4, 5, 1, 299.99),
(5, 9, 1, 189.99);

-- Insert Sample Reviews
INSERT INTO Reviews (user_id, product_id, rating, comment) VALUES
(1, 1, 5, 'Excellent laptop! Very fast and reliable.'),
(2, 2, 4, 'Great sound quality, comfortable fit.'),
(3, 4, 5, 'Perfect running shoes, very comfortable!'),
(4, 5, 4, 'Good quality jacket, keeps me warm.'),
(1, 11, 5, 'Amazing smartwatch, love the battery life!'),
(2, 7, 4, 'Bright and adjustable lamp, works great.'),
(3, 9, 5, 'Professional quality racket, highly recommend.'),
(4, 10, 4, 'Informative and well-written book.'),
(5, 3, 5, 'Fast charging cable, great quality.'),
(1, 6, 4, 'Very comfortable office chair, good support.');

SHOW TABLES;
SELECT 'Sample data inserted successfully!' AS Status;

-- Update existing admin user role to uppercase
UPDATE Users SET role = 'ADMIN' WHERE email = 'admin@test.com';

-- Verify the update
SELECT user_id, name, email, role FROM Users WHERE email = 'admin@test.com';
