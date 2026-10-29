-- ============================================================================
-- MASTER RESTAURANT DATABASE LIFECYCLE INITIALIZATION SCHEMA
-- ============================================================================

DROP TABLE IF EXISTS payment CASCADE;
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS customer_order CASCADE;
DROP TABLE IF EXISTS menu_items CASCADE;
DROP TABLE IF EXISTS dining_table CASCADE;
DROP TABLE IF EXISTS worker CASCADE;

-- 1. WORKER/STAFF DATA STRUCTURE
CREATE TABLE worker (
    worker_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. PHYSICAL DINING TABLES DATA STRUCTURE
CREATE TABLE dining_table (
    table_id SERIAL PRIMARY KEY,
    table_number INT NOT NULL UNIQUE,
    seating_capacity INT NOT NULL DEFAULT 4,
    table_status VARCHAR(50) DEFAULT 'available', -- Matches: 'occupied' / 'available'
    assigned_waiter_id VARCHAR(50) DEFAULT 'None'
);

-- 3. FOOD & BEVERAGE MENU CATALOG DATA STRUCTURE
CREATE TABLE menu_items (
    item_id SERIAL PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL UNIQUE,
    item_description TEXT,
    price NUMERIC(6, 2) NOT NULL,
    category VARCHAR(50) NOT NULL
);

-- 4. LIVE TRANSACTION CUSTOMER ORDERS DATA STRUCTURE
CREATE TABLE customer_order (
    order_id VARCHAR(255) PRIMARY KEY,
    table_id INT NOT NULL REFERENCES dining_table(table_id) ON DELETE RESTRICT,
    order_status VARCHAR(50) NOT NULL DEFAULT 'open', -- Matches: 'open' / 'closed'
    order_creation TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(8, 2) DEFAULT 0.00
);

-- 5. MULTI-ITEM ORDER SUB-ITEMS MAPS
CREATE TABLE order_item (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES menu_items(item_id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0)
);

-- 6. AUDITING ACCOUNTING PAYMENTS DATA STRUCTURE
CREATE TABLE payment (
    payment_id SERIAL PRIMARY KEY,
    order_id VARCHAR(255) REFERENCES customer_order(order_id) ON DELETE SET NULL,
    amount NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    tip NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    payment_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================================
-- PRODUCTION ASSIGNMENT DEMO SEED RECORDS
-- ============================================================================

-- Seed System Waiters
INSERT INTO worker (name, is_active) VALUES 
('Waiter Grace', true),
('Waiter Jackson', true),
('Staff Alex', false);

-- Seed System Dining Tables (Populates 'Occupied Table' loops)
INSERT INTO dining_table (table_number, seating_capacity, table_status) VALUES
(1, 2, 'occupied'),
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
