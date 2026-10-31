-- ============================================================================
-- GREEN OLIVES RESTAURANT DATABASE INITIALIZATION SCHEMA & TEST DATA
-- Target Dialect: PostgreSQL 16
-- ============================================================================

-- Clean up any existing legacy tables to ensure a completely pristine state
DROP TABLE IF EXISTS payment CASCADE;
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS customer_order CASCADE;
DROP TABLE IF EXISTS menu_items CASCADE;
DROP TABLE IF EXISTS dining_table CASCADE;
DROP TABLE IF EXISTS worker CASCADE;

-- 1. WORKER TABLE
CREATE TABLE worker (
    worker_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'MANAGER', 'CHEF', 'WAITER', 'HOST'
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- 2. DINING_TABLE TABLE (Restored to table_number to align with your Java API)
CREATE TABLE dining_table (
    table_number INT PRIMARY KEY,
    capacity INT NOT NULL,
    table_status VARCHAR(50) DEFAULT 'available' NOT NULL -- 'available', 'occupied', 'reserved'
);

-- 3. MENU_ITEMS TABLE
CREATE TABLE menu_items (
    item_id SERIAL PRIMARY KEY,
    item_name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'Appetizer', 'Entree', 'Dessert', 'Beverage'
    price DECIMAL(10, 2) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE NOT NULL
);

-- 4. CUSTOMER_ORDER TABLE
CREATE TABLE customer_order (
    order_id SERIAL PRIMARY KEY,
    table_number INT NOT NULL REFERENCES dining_table(table_number) ON DELETE RESTRICT,
    waiter_id INT REFERENCES worker(worker_id) ON DELETE SET NULL,
    order_status VARCHAR(50) DEFAULT 'open' NOT NULL, -- 'open', 'preparing', 'served', 'closed', 'cancelled'
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. ORDER_ITEM TABLE (Junction Table for Orders & Menu Items)
CREATE TABLE order_item (
    order_item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES menu_items(item_id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0),
    special_notes TEXT
);

-- 6. PAYMENT TABLE
CREATE TABLE payment (
    payment_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL UNIQUE REFERENCES customer_order(order_id) ON DELETE RESTRICT,
    amount DECIMAL(10, 2) NOT NULL,
    tip DECIMAL(10, 2) DEFAULT 0.00 NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- 'Cash', 'Credit Card', 'Mobile Pay'
    payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================================
-- COMPREHENSIVE PRODUCTION TEST DATA INSERTIONS
-- ============================================================================

-- Seed Workers (Staff Members)
INSERT INTO worker (name, role, is_active) VALUES 
('Alice Smith', 'MANAGER', true),
('Bob Jones', 'CHEF', true),
('Charlie Brown', 'WAITER', true),
('Diana Prince', 'HOST', true),
('Evan Wright', 'WAITER', true);

-- Seed Restaurant Floor Layout (Tables - Fixed parameter identifier)
INSERT INTO dining_table (table_number, capacity, table_status) VALUES 
(1, 2, 'occupied'),
(2, 4, 'available'),
(3, 4, 'occupied'),
(4, 6, 'available'),
(5, 2, 'reserved');

-- Seed the Culinary Menu Items
INSERT INTO menu_items (item_name, category, price, is_available) VALUES 
('Garlic Bread with Cheese', 'Appetizer', 6.99, true),
('Stuffed Mushrooms', 'Appetizer', 8.50, true),
('Grilled Salmon Steaks', 'Entree', 24.99, true),
('Prime Rib Eye Steak', 'Entree', 29.99, true),
('Fettuccine Alfredo', 'Entree', 18.99, true),
('Warm Chocolate Lava Cake', 'Dessert', 7.50, true),
('New York Cheesecake', 'Dessert', 6.99, true),
('Iced Lemon Tea', 'Beverage', 2.99, true),
('Craft IPA Beer', 'Beverage', 6.50, true);

-- Seed Live Open & Preparing Customer Orders
INSERT INTO customer_order (table_number, waiter_id, order_status, order_time) VALUES 
(1, 3, 'preparing', CURRENT_TIMESTAMP - INTERVAL '25 minutes'),
(3, 5, 'open', CURRENT_TIMESTAMP - INTERVAL '10 minutes');

-- Link Ordered Items to the Active Receipts
INSERT INTO order_item (order_id, item_id, quantity, special_notes) VALUES 
(1, 1, 1, 'Extra cheese please'),  
(1, 4, 1, 'Medium rare'),          
(1, 8, 2, NULL),                   
(2, 2, 1, NULL),                   
(2, 5, 2, 'No mushrooms in sauce');

-- Seed Historically Closed Orders & Completed Invoices
INSERT INTO customer_order (table_number, waiter_id, order_status, order_time) VALUES 
(2, 3, 'closed', CURRENT_TIMESTAMP - INTERVAL '2 hours');

INSERT INTO order_item (order_id, item_id, quantity, special_notes) VALUES 
(3, 3, 1, NULL),                   
(3, 9, 1, NULL);                   

INSERT INTO payment (order_id, amount, tip, payment_method, payment_time) VALUES 
(3, 31.49, 6.00, 'Credit Card', CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes');
