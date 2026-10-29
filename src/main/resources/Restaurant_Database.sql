DROP TABLE IF EXISTS manager_overrides;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS restaurant_orders;
DROP TABLE IF EXISTS dining_tables;
DROP TABLE IF EXISTS staff_roster;

-- Tracks personnel records across the 5 mandatory capstone roles
CREATE TABLE staff_roster (
    employee_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    passcode VARCHAR(50) NOT NULL,
    assigned_role VARCHAR(20) NOT NULL -- 'HOST', 'WAITER', 'CHEF', 'MANAGER', 'OWNER'
);

-- Tracks physical dining tables on the floor
CREATE TABLE dining_tables (
    table_id INT PRIMARY KEY,
    capacity INT NOT NULL,
    is_occupied BOOLEAN DEFAULT FALSE,
    assigned_waiter_id VARCHAR(50) DEFAULT NULL,
    FOREIGN KEY (assigned_waiter_id) REFERENCES staff_roster(employee_id)
);

-- Tracks active customer tickets through operational states
CREATE TABLE restaurant_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    table_id INT NOT NULL,
    status VARCHAR(30) DEFAULT 'ORDERED', -- 'ORDERED', 'IN_PROGRESS', 'DONE', 'PAID'
    beverage_refill_count INT DEFAULT 0,
    subtotal DOUBLE DEFAULT 0.0,
    tip_amount DOUBLE DEFAULT 0.0,
    FOREIGN KEY (table_id) REFERENCES dining_tables(table_id)
);

-- Active management override exception logging window
CREATE TABLE manager_overrides (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    requesting_staff_id VARCHAR(50) NOT NULL,
    table_id INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING_APPROVAL', -- 'PENDING_APPROVAL', 'APPROVED', 'DENIED'
    resolved_by VARCHAR(50) DEFAULT NULL,
    FOREIGN KEY (requesting_staff_id) REFERENCES staff_roster(employee_id),
    FOREIGN KEY (resolved_by) REFERENCES staff_roster(employee_id)
);

-- Seed production terminal test accounts
INSERT INTO staff_roster (employee_id, name, passcode, assigned_role) VALUES 
('EMP-HOST01', 'Alex Smith', '1111', 'HOST'),
('EMP-WAIT01', 'Sarah Connor', '2222', 'WAITER'),
('EMP-CHEF01', 'Marcus Wright', '3333', 'CHEF'),
('EMP-MGR01', 'Boss Lady', '4444', 'MANAGER'),
('EMP-OWNER01', 'John Doe', '5555', 'OWNER');

-- Initialize dining tables
INSERT INTO dining_tables (table_id, capacity) VALUES 
(1, 2), (2, 2), (3, 4), (4, 4), (5, 6), (6, 6), (7, 8), (8, 4);
