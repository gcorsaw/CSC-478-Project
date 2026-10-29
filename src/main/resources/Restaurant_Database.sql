-- Drop tables if they exist to allow a clean schema initialization cascade
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS customer_order CASCADE;
DROP TABLE IF EXISTS menu_items CASCADE;
DROP TABLE IF EXISTS dining_table CASCADE;
DROP TABLE IF EXISTS worker CASCADE;
DROP TABLE IF EXISTS payment CASCADE;

-- 1. WORKER RELATION (Required by line 53 summary method query block)
CREATE TABLE worker (
    worker_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. DINING TABLE RELATION (Matches table_status metrics fields exactly)
CREATE TABLE dining_table (
    table_id SERIAL PRIMARY KEY,
    table_number INT NOT NULL UNIQUE,
    seating_capacity INT NOT NULL DEFAULT 4,
    table_status VARCHAR(50) DEFAULT 'available', -- Expected by code filter: 'occupied' / 'available'
    assigned_waiter_id VARCHAR(50) DEFAULT 'None'
);

-- 3. MENU ITEMS CATALOUGE (Provides lookups matching your item_id strings)
CREATE TABLE menu_items (
    item_id SERIAL PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL UNIQUE,
    item_description TEXT,
    price NUMERIC(6, 2) NOT NULL,
    category VARCHAR(50) NOT NULL
);

-- 4. CUSTOMER ORDER LOGISTICAL STATES (Matches your co.order_status strings)
CREATE TABLE customer_order (
    order_id VARCHAR(255) PRIMARY KEY,
    table_id INT NOT NULL REFERENCES dining_table(table_id) ON DELETE RESTRICT,
    order_status VARCHAR(50) NOT NULL DEFAULT 'open', -- Expected by code filter: 'open' / 'closed'
    order_creation TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(8, 2) DEFAULT 0.00
);

-- 5. ORDER ITEM JOIN LINE DETAILS 
CREATE TABLE order_item (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES menu_items(item_id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0)
);

-- 6. PAYMENT ACCOUNTING BALANCES (Required by sum metrics aggregate)
CREATE TABLE payment (
    payment_id SERIAL PRIMARY KEY,
    order_id VARCHAR(255) REFERENCES customer_order(order_id) ON DELETE SET NULL,
    amount NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    tip NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    payment_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================================
-- SEED COHESIVE SYSTEM DEMO RECORDS 
-- ============================================================================

-- Seed Active Staff members
INSERT INTO worker (name, is_active) VALUES 
('Alice Smith', true),
('Bob Jones', true),
('Charlie Adams', false);

-- Seed System Dining Tables
INSERT INTO dining_table (table_number, seating_capacity, table_status) VALUES
(1, 2, 'available'),
(2, 4, 'occupied'),
(3, 4, 'available'),
(4, 6, 'occupied');

-- Seed Complete Menu Categories
INSERT INTO menu_items (item_name, item_description, price, category) VALUES
('Stuffed Grape Leaves', 'Rice and herb filled Mediterranean style rolls.', 8.50, 'Appetizer'),
('Charbroiled Ribeye Steak', '12oz choice cut beef ribeye grilled over open coals.', 34.00, 'Entree'),
('Fettuccine Green Olives', 'Rich house white cream garlic reduction over noodles.', 21.00, 'Entree');

-- Seed Pipeline customer orders
INSERT INTO customer_order (order_id, table_id, order_status) VALUES
('ord-x92f-104a', 2, 'open'),
('ord-z31b-749c', 4, 'open');

-- Attach line items
INSERT INTO order_item (order_id, item_id, quantity) VALUES
('ord-x92f-104a', 1, 1),
('ord-x92f-104a', 2, 1),
('ord-z31b-749c', 3, 2);

-- Seed base payment metrics history
INSERT INTO payment (amount, tip) VALUES 
(45.50, 8.00),
(22.00, 4.50);
