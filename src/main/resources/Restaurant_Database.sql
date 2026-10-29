--Employee/People Tables--
create table if not exists worker(
    worker_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name text not null,
    last_name text not null,
    username text not null unique,
    password_hash text not null,
    is_active boolean not null default true,
    worker_creation timestamp not null DEFAULT now(),
    worker_update timestamp not null default now()
);

create table if not exists host_station(
    host_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    worker_id int not null references worker(worker_id) on delete cascade,
    host_creation timestamp not null default now(),
    host_update timestamp not null DEFAULT now (),
    unique(worker_id)
);

create table if not exists waiter_station(
    waiter_id int generated always as identity primary key,
    worker_id int not null references worker(worker_id) on delete cascade,
    waiter_creation timestamp not null default now(),
    waiter_update timestamp not null default now(),
    unique(worker_id)
);

create table if not exists chef_station(
    chef_id int generated always as identity primary key,
    worker_id int not null references worker(worker_id) on delete cascade,
    chef_creation timestamp not null default now(),
    chef_update timestamp not null default now(),
    unique(worker_id)
);

create table if not exists manager_station(
    manager_id int generated always as identity primary key,
    worker_id int not null references worker(worker_id) on delete cascade,
    manager_creation timestamp not null default now(),
    manager_update timestamp not null default now(),
    unique(worker_id)

);

create table if not exists owner_station(
    owner_id int generated always as identity primary key,
    worker_id int not null references worker(worker_id) on delete cascade,
    owner_creation TIMESTAMP not null default now(),
    owner_update TIMESTAMP not null default now(),
    unique(worker_id)
);

create table if not exists admin_station(
    admin_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    worker_id int not null references worker(worker_id) on delete cascade,
    admin_creation timestamp not null default now(),
    admin_update timestamp not null default now(),
    unique(worker_id)
);

-- Dining Areas --
create table if not exists dining_table(
    table_id int generated always as identity primary key,
    table_number int not null unique,
    seats int not null check (seats > 0),
    table_status text not null default 'open'
        check (table_status in ('open', 'occupied')),
    guest_name text not null default '',
    party_size int not null default 0 check (party_size >= 0),
    table_update timestamp not null default now()
);

alter table dining_table add column if not exists guest_name text not null default '';
alter table dining_table add column if not exists party_size int not null default 0;

create table if not exists table_assignment(
    table_id int not null references dining_table(table_id) on delete cascade,
    waiter_id int not null references waiter_station(waiter_id) on delete cascade,
    assigned_at timestamp not null default now(),
    primary key(table_id, waiter_id)
);

-- Menu Items --
create table if not exists menu_items(
    item_id int generated always as identity primary key,
    item_name text not null,
    category text not null check (category in ('starter', 'main', 'dessert', 'drink')),
    price numeric(8,2) not null check (price >= 0),
    is_available boolean not null default true,
    item_creation timestamp not null default now(),
    item_update timestamp not null default now()
);

create table if not exists customer_order(
    order_id int generated always as identity primary key,
    table_id int not null references dining_table(table_id),
    waiter_id int not null references waiter_station(waiter_id),
    order_status text not null default 'open' check (order_status in ('open', 'closed')),
    sample_order boolean not null default false,
    order_creation timestamp not null default now(),
    order_update timestamp not null default now()
);

alter table customer_order add column if not exists sample_order boolean not null default false;

create table if not exists order_item(
    order_item_id int generated always as identity primary key,
    order_id int not null references customer_order(order_id) on delete cascade,
    item_id int not null references menu_items(item_id),
    quantity int not null default 1 check (quantity > 0),
    notes text,
    kitchen_status text not null default 'standing_by'
        check (kitchen_status in ('standing_by', 'in_progress', 'done')),
    priority int not null default 0,
    item_creation timestamp not null default now(),
    item_update timestamp not null default now()
);

create table if not exists payment(
    payment_id int generated always as identity primary key,
    order_id int not null references customer_order(order_id),
    amount numeric(10,2) not null check(amount >= 0),
    tip numeric(10,2) not null default 0 check (tip >= 0),
    paid_at timestamp not null default now()
);

DROP TABLE IF EXISTS manager_overrides;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS staff_roster;

-- Core table storing personnel records across the 5 mandatory capstone roles
CREATE TABLE staff_roster (
    employee_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    passcode VARCHAR(50) NOT NULL,
    assigned_role VARCHAR(20) NOT NULL -- 'HOST', 'WAITER', 'CHEF', 'MANAGER', 'OWNER'
);

-- Active management override logging window
CREATE TABLE manager_overrides (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    requesting_staff_id VARCHAR(50) NOT NULL,
    table_id INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING_APPROVAL',
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

-- Sample data for tables--
insert into worker (first_name, last_name, username, password_hash) values
    ('Jane', 'Doe',    'jdoe',   'hash_placeholder_1'),   -- waiter + manager
    ('Sam',  'Lee',    'slee',   'hash_placeholder_2'),   -- waiter only
    ('Alex', 'Kim',    'akim',   'hash_placeholder_3'),   -- chef
    ('Pat',  'Nguyen', 'pnguyen','hash_placeholder_4'),   -- host
    ('Robin','Patel',  'rpatel', 'hash_placeholder_5')   -- owner
on conflict (username) do nothing;
 
insert into waiter_station (worker_id) select worker_id from worker where username in ('jdoe','slee')
on conflict (worker_id) do nothing;
insert into manager_station (worker_id) select worker_id from worker where username = 'jdoe'
on conflict (worker_id) do nothing;
insert into chef_station (worker_id) select worker_id from worker where username = 'akim'
on conflict (worker_id) do nothing;
insert into host_station (worker_id) select worker_id from worker where username = 'pnguyen'
on conflict (worker_id) do nothing;
insert into owner_station (worker_id) select worker_id from worker where username = 'rpatel'
on conflict (worker_id) do nothing;
 
-- Dining tables
insert into dining_table (table_number, seats) values
    (1, 2),
    (2, 4),
    (3, 4),
    (4, 6),
    (5, 2),
    (6, 4),
    (7, 6),
    (8, 4)
on conflict (table_number) do nothing;
 
-- Table assignments (manager assigns waiters to tables)
insert into table_assignment (table_id, waiter_id)
    select dt.table_id, ws.waiter_id
    from dining_table dt
    cross join waiter_station ws
    join worker w on w.worker_id = ws.worker_id
    where (dt.table_number in (1,2) and w.username = 'jdoe')
       or (dt.table_number in (3,4) and w.username = 'slee')
on conflict (table_id, waiter_id) do nothing;
 
-- Menu
insert into menu_items (item_name, category, price)
select sample.item_name, sample.category, sample.price
from (values
    ('Caesar Salad',      'starter', 9.50),
    ('Garlic Bread',      'starter', 6.00),
    ('Grilled Salmon',    'main',   22.00),
    ('Margherita Pizza',  'main',   15.50),
    ('Tiramisu',          'dessert', 7.50),
    ('Iced Tea',          'drink',   3.00),
    ('Sparkling Water',   'drink',   2.50),
    ('Garden Salad',      'starter', 6.99),
    ('Soup of the Day',   'starter', 4.99),
    ('Grilled Chicken',   'main',    8.99),
    ('Classic Cheeseburger', 'main', 9.99),
    ('Vegetable Pasta',   'main',   10.99),
    ('Chocolate Cake',    'dessert', 6.99),
    ('Ice Cream',         'dessert', 4.99),
    ('Cheesecake',        'dessert', 5.99),
    ('Coffee',            'drink',   1.99),
    ('Tea',               'drink',   2.49),
    ('Any Soda',          'drink',   2.99)
) as sample(item_name, category, price)
where not exists (
    select 1 from menu_items existing where existing.item_name = sample.item_name
);

-- Insert the sample order once; normal app orders are never reseeded or closed on startup.
insert into customer_order (table_id, waiter_id, sample_order)
    select dt.table_id, ws.waiter_id, true
    from dining_table dt
    cross join waiter_station ws
    join worker w on w.worker_id = ws.worker_id
    where dt.table_number = 1 and w.username = 'jdoe'
      and not exists (select 1 from customer_order where sample_order);
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 1, 'in_progress'
    from customer_order co
    cross join menu_items m
    join dining_table dt on co.table_id = dt.table_id
    where co.sample_order and co.order_status = 'open' and m.item_name = 'Caesar Salad'
      and not exists (select 1 from order_item oi where oi.order_id = co.order_id and oi.item_id = m.item_id);
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 2, 'standing_by'
    from customer_order co
    cross join menu_items m
    join dining_table dt on co.table_id = dt.table_id
    where co.sample_order and co.order_status = 'open' and m.item_name = 'Margherita Pizza'
      and not exists (select 1 from order_item oi where oi.order_id = co.order_id and oi.item_id = m.item_id);
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 2, 'done'
    from customer_order co
    cross join menu_items m
    join dining_table dt on co.table_id = dt.table_id
    where co.sample_order and co.order_status = 'open' and m.item_name = 'Iced Tea'
      and not exists (select 1 from order_item oi where oi.order_id = co.order_id and oi.item_id = m.item_id);
 
-- Payment closing out that order
insert into payment (order_id, amount, tip)
    select co.order_id, 46.50, 8.00 
    from customer_order co
    join dining_table dt on co.table_id = dt.table_id
    where co.sample_order and co.order_status = 'open'
      and not exists (select 1 from payment p where p.order_id = co.order_id);
 
update customer_order 
set order_status = 'closed' 
where sample_order and order_status = 'open';

-- New Test cases 
insert into admin_station(worker_id) select worker_id from worker where username = 'rpatel'
on conflict (worker_id) do nothing;

insert into worker(first_name, last_name, username, password_hash, is_active) values
('Former', 'Employee', 'fformer','hash_placeholder_6', false)
on conflict (username) do nothing;