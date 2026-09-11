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
    table_update timestamp not null default now()
);

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
    order_creation timestamp not null default now(),
    order_update timestamp not null default now()
);

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

-- Sample data for tables--
insert into worker (first_name, last_name, username, password_hash) values
    ('Jane', 'Doe',    'jdoe',   'hash_placeholder_1'),   -- waiter + manager
    ('Sam',  'Lee',    'slee',   'hash_placeholder_2'),   -- waiter only
    ('Alex', 'Kim',    'akim',   'hash_placeholder_3'),   -- chef
    ('Pat',  'Nguyen', 'pnguyen','hash_placeholder_4'),   -- host
    ('Robin','Patel',  'rpatel', 'hash_placeholder_5');   -- owner
 
insert into waiter_station (worker_id) select worker_id from worker where username in ('jdoe','slee');
insert into manager_station (worker_id) select worker_id from worker where username = 'jdoe';
insert into chef_station (worker_id) select worker_id from worker where username = 'akim';
insert into host_station (worker_id) select worker_id from worker where username = 'pnguyen';
insert into owner_station (worker_id) select worker_id from worker where username = 'rpatel';
 
-- Dining tables
insert into dining_table (table_number, seats) values
    (1, 2),
    (2, 4),
    (3, 4),
    (4, 6);
 
-- Table assignments (manager assigns waiters to tables)
insert into table_assignment (table_id, waiter_id)
    select dt.table_id, ws.waiter_id
    from dining_table dt, waiter_station ws
    join worker w on w.worker_id = ws.worker_id
    where (dt.table_number in (1,2) and w.username = 'jdoe')
       or (dt.table_number in (3,4) and w.username = 'slee');
 
-- Menu
insert into menu_items (item_name, category, price) values
    ('Caesar Salad',      'starter', 9.50),
    ('Garlic Bread',      'starter', 6.00),
    ('Grilled Salmon',    'main',   22.00),
    ('Margherita Pizza',  'main',   15.50),
    ('Tiramisu',          'dessert', 7.50),
    ('Iced Tea',          'drink',   3.00),
    ('Sparkling Water',   'drink',   2.50);
 
-- Mark table 1 occupied since we're about to seat an order there
update dining_table set table_status = 'occupied' where table_number = 1;
 
-- An order at table 1, taken by Jane (jdoe)
insert into customer_order (table_id, waiter_id)
    select dt.table_id, ws.waiter_id
    from dining_table dt, waiter_station ws
    join worker w on w.worker_id = ws.worker_id
    where dt.table_number = 1 and w.username = 'jdoe';
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 1, 'in_progress'
    from customer_order co, menu_items m
    where co.order_id = 1 and m.item_name = 'Caesar Salad';
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 2, 'standing_by'
    from customer_order co, menu_items m
    where co.order_id = 1 and m.item_name = 'Margherita Pizza';
 
insert into order_item (order_id, item_id, quantity, kitchen_status)
    select co.order_id, m.item_id, 2, 'done'
    from customer_order co, menu_items m
    where co.order_id = 1 and m.item_name = 'Iced Tea';
 
-- Payment closing out that order
insert into payment (order_id, amount, tip)
    select order_id, 46.50, 8.00 from customer_order where order_id = 1;
 
update customer_order set order_status = 'closed' where order_id = 1;
update dining_table set table_status = 'open' where table_number = 1;