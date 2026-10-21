package csc478project.restaurantapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

@RestController
@RequestMapping("/api")
public class RestaurantApiController {
    private final JdbcTemplate jdbc;

    public RestaurantApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/tables")
    public List<TableView> tables() {
        return jdbc.query("""
                SELECT table_number, seats, table_status, guest_name, party_size
                FROM dining_table
                ORDER BY table_number
                """,
                (rs, rowNum) -> new TableView(
                        rs.getInt("table_number"),
                        rs.getInt("seats"),
                        rs.getString("table_status"),
                        rs.getString("guest_name"),
                        rs.getInt("party_size")));
    }

    @GetMapping("/summary")
    public SummaryView summary() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT count(*) FROM worker WHERE is_active) AS active_workers,
                    (SELECT count(*) FROM dining_table) AS total_tables,
                    (SELECT count(*) FROM dining_table WHERE table_status = 'occupied') AS occupied_tables,
                    (SELECT count(*) FROM customer_order) AS total_orders,
                    (SELECT count(*) FROM customer_order WHERE order_status = 'open') AS open_orders,
                    (SELECT COALESCE(sum(amount + tip), 0) FROM payment) AS collected
                """,
                (rs, rowNum) -> new SummaryView(
                        rs.getLong("active_workers"),
                        rs.getLong("total_tables"),
                        rs.getLong("occupied_tables"),
                        rs.getLong("total_orders"),
                        rs.getLong("open_orders"),
                        rs.getBigDecimal("collected")));
    }

    @GetMapping("/admin/health")
    public HealthView health() {
        return jdbc.queryForObject("""
                SELECT current_database() AS database_name,
                       current_timestamp AS checked_at,
                       (SELECT count(*) FROM worker) AS worker_count,
                       (SELECT count(*) FROM dining_table) AS table_count,
                       (SELECT count(*) FROM customer_order) AS order_count
                """,
                (rs, rowNum) -> new HealthView(
                        rs.getString("database_name"),
                        rs.getTimestamp("checked_at").toInstant().toString(),
                        rs.getLong("worker_count"),
                        rs.getLong("table_count"),
                        rs.getLong("order_count")));
    }

    @GetMapping("/workers")
    public List<WorkerView> workers() {
        return jdbc.query("""
                SELECT w.worker_id, w.first_name, w.last_name, w.username, w.is_active,
                       array_remove(ARRAY[
                           CASE WHEN hs.host_id IS NOT NULL THEN 'Host' END,
                           CASE WHEN ws.waiter_id IS NOT NULL THEN 'Waiter' END,
                           CASE WHEN cs.chef_id IS NOT NULL THEN 'Chef' END,
                           CASE WHEN ms.manager_id IS NOT NULL THEN 'Manager' END,
                           CASE WHEN os.owner_id IS NOT NULL THEN 'Owner' END,
                           CASE WHEN ads.admin_id IS NOT NULL THEN 'Admin' END
                       ], NULL) AS roles
                FROM worker w
                LEFT JOIN host_station hs ON hs.worker_id = w.worker_id
                LEFT JOIN waiter_station ws ON ws.worker_id = w.worker_id
                LEFT JOIN chef_station cs ON cs.worker_id = w.worker_id
                LEFT JOIN manager_station ms ON ms.worker_id = w.worker_id
                LEFT JOIN owner_station os ON os.worker_id = w.worker_id
                LEFT JOIN admin_station ads ON ads.worker_id = w.worker_id
                ORDER BY w.last_name, w.first_name
                """,
                (rs, rowNum) -> new WorkerView(
                        rs.getInt("worker_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("username"),
                        rs.getBoolean("is_active"),
                        List.of((String[]) rs.getArray("roles").getArray())));
    }

    @GetMapping("/waiters")
    public List<WaiterView> waiters() {
        return jdbc.query("""
                SELECT ws.waiter_id, w.first_name, w.last_name, w.is_active
                FROM waiter_station ws
                JOIN worker w ON w.worker_id = ws.worker_id
                ORDER BY w.last_name, w.first_name
                """,
                (rs, rowNum) -> new WaiterView(
                        rs.getInt("waiter_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getBoolean("is_active")));
    }

    @GetMapping("/table-assignments")
    public List<TableAssignmentView> assignments() {
        return jdbc.query("""
                SELECT dt.table_number, ws.waiter_id, w.first_name, w.last_name
                FROM table_assignment ta
                JOIN dining_table dt ON dt.table_id = ta.table_id
                JOIN waiter_station ws ON ws.waiter_id = ta.waiter_id
                JOIN worker w ON w.worker_id = ws.worker_id
                ORDER BY dt.table_number, w.last_name, w.first_name
                """,
                (rs, rowNum) -> new TableAssignmentView(
                        rs.getInt("table_number"),
                        rs.getInt("waiter_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name")));
    }

    @PostMapping("/table-assignments")
    public TableAssignmentView assignWaiter(@RequestBody AssignWaiterRequest request) {
        if (request == null || request.tableNumber() == null || request.tableNumber() < 1
                || request.waiterId() == null || request.waiterId() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a table and an active waiter.");
        }

        List<TableAssignmentView> result = jdbc.query("""
                INSERT INTO table_assignment (table_id, waiter_id)
                SELECT dt.table_id, ws.waiter_id
                FROM dining_table dt
                JOIN waiter_station ws ON ws.waiter_id = ?
                JOIN worker w ON w.worker_id = ws.worker_id AND w.is_active
                WHERE dt.table_number = ?
                ON CONFLICT (table_id, waiter_id) DO NOTHING
                RETURNING table_id, waiter_id
                """, (rs, rowNum) -> new TableAssignmentView(
                        request.tableNumber(), rs.getInt("waiter_id"), "", ""),
                request.waiterId(), request.tableNumber());
        if (!result.isEmpty()) {
            WaiterView waiter = jdbc.queryForObject("""
                    SELECT ws.waiter_id, w.first_name, w.last_name, w.is_active
                    FROM waiter_station ws JOIN worker w ON w.worker_id = ws.worker_id
                    WHERE ws.waiter_id = ?
                    """, (rs, rowNum) -> new WaiterView(
                            rs.getInt("waiter_id"), rs.getString("first_name"),
                            rs.getString("last_name"), rs.getBoolean("is_active")),
                    request.waiterId());
            return new TableAssignmentView(request.tableNumber(), waiter.waiterId(),
                    waiter.firstName(), waiter.lastName());
        }

        Boolean tableExists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM dining_table WHERE table_number = ?)",
                Boolean.class, request.tableNumber());
        Boolean waiterExists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM waiter_station ws JOIN worker w ON w.worker_id = ws.worker_id
                    WHERE ws.waiter_id = ? AND w.is_active
                )
                """, Boolean.class, request.waiterId());
        if (!Boolean.TRUE.equals(tableExists)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That table does not exist.");
        }
        if (!Boolean.TRUE.equals(waiterExists)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That active waiter does not exist.");
        }
        return jdbc.queryForObject("""
                SELECT dt.table_number, ws.waiter_id, w.first_name, w.last_name
                FROM table_assignment ta
                JOIN dining_table dt ON dt.table_id = ta.table_id
                JOIN waiter_station ws ON ws.waiter_id = ta.waiter_id
                JOIN worker w ON w.worker_id = ws.worker_id
                WHERE dt.table_number = ? AND ws.waiter_id = ?
                """, (rs, rowNum) -> new TableAssignmentView(
                        rs.getInt("table_number"), rs.getInt("waiter_id"),
                        rs.getString("first_name"), rs.getString("last_name")),
                request.tableNumber(), request.waiterId());
    }

    @GetMapping("/kitchen/orders")
    public List<KitchenItemView> kitchenOrders() {
        return jdbc.query("""
                SELECT co.order_id, oi.order_item_id, dt.table_number, mi.item_name,
                       oi.quantity, oi.notes, oi.kitchen_status, oi.priority, oi.item_creation
                FROM order_item oi
                JOIN customer_order co ON co.order_id = oi.order_id
                JOIN dining_table dt ON dt.table_id = co.table_id
                JOIN menu_items mi ON mi.item_id = oi.item_id
                WHERE co.order_status = 'open' AND oi.kitchen_status <> 'done'
                ORDER BY oi.priority DESC, oi.item_creation, oi.order_item_id
                """,
                (rs, rowNum) -> new KitchenItemView(
                        rs.getInt("order_id"),
                        rs.getInt("order_item_id"),
                        rs.getInt("table_number"),
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getString("notes"),
                        rs.getString("kitchen_status"),
                        rs.getInt("priority")));
    }

    @PostMapping("/kitchen/items/{itemId}/status")
    public KitchenItemView updateKitchenStatus(
            @PathVariable int itemId,
            @RequestBody KitchenStatusRequest request) {
        if (request == null || request.status() == null
                || !List.of("standing_by", "in_progress", "done").contains(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a valid kitchen status.");
        }
        int updated = jdbc.update("""
                UPDATE order_item oi
                SET kitchen_status = ?, item_update = now()
                FROM customer_order co
                WHERE oi.order_item_id = ? AND co.order_id = oi.order_id
                  AND co.order_status = 'open'
                """, request.status(), itemId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "That open kitchen item does not exist.");
        }
        return jdbc.queryForObject("""
                SELECT co.order_id, oi.order_item_id, dt.table_number, mi.item_name,
                       oi.quantity, oi.notes, oi.kitchen_status, oi.priority
                FROM order_item oi
                JOIN customer_order co ON co.order_id = oi.order_id
                JOIN dining_table dt ON dt.table_id = co.table_id
                JOIN menu_items mi ON mi.item_id = oi.item_id
                WHERE oi.order_item_id = ?
                """,
                (rs, rowNum) -> new KitchenItemView(
                        rs.getInt("order_id"), rs.getInt("order_item_id"),
                        rs.getInt("table_number"), rs.getString("item_name"),
                        rs.getInt("quantity"), rs.getString("notes"),
                        rs.getString("kitchen_status"), rs.getInt("priority")),
                itemId);
    }

    @PostMapping("/tables")
    public TableView addTable(@RequestBody AddTableRequest request) {
        if (request == null || request.tableNumber() == null || request.capacity() == null
                || request.tableNumber() < 1 || request.capacity() < 1 || request.capacity() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a positive table number and a capacity from 1 to 20.");
        }

        try {
            jdbc.update("""
                    INSERT INTO dining_table (table_number, seats)
                    VALUES (?, ?)
                    """, request.tableNumber(), request.capacity());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That table number already exists.");
        }
        return table(request.tableNumber());
    }

    @PostMapping("/seatings")
    @Transactional
    public TableView seat(@RequestBody SeatRequest request) {
        if (request == null || request.guestName() == null || request.guestName().isBlank()
                || request.guestName().trim().length() > 60
                || request.partySize() == null || request.partySize() < 1 || request.partySize() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a guest name (up to 60 characters) and a party size from 1 to 20.");
        }

        String guestName = request.guestName().trim();
        List<TableView> seated;
        if (request.tableNumber() != null) {
            seated = jdbc.query("""
                    UPDATE dining_table
                    SET table_status = 'occupied', guest_name = ?, party_size = ?, table_update = now()
                    WHERE table_number = ? AND table_status = 'open' AND seats >= ?
                    RETURNING table_number, seats, table_status, guest_name, party_size
                    """,
                    (rs, rowNum) -> new TableView(
                            rs.getInt("table_number"),
                            rs.getInt("seats"),
                            rs.getString("table_status"),
                            rs.getString("guest_name"),
                            rs.getInt("party_size")),
                    guestName, request.partySize(), request.tableNumber(), request.partySize());
        } else {
            seated = jdbc.query("""
                    WITH candidate AS (
                        SELECT table_id
                        FROM dining_table
                        WHERE table_status = 'open' AND seats >= ?
                        ORDER BY seats, table_number
                        FOR UPDATE SKIP LOCKED
                        LIMIT 1
                    )
                    UPDATE dining_table AS target
                    SET table_status = 'occupied', guest_name = ?, party_size = ?, table_update = now()
                    FROM candidate
                    WHERE target.table_id = candidate.table_id
                    RETURNING target.table_number, target.seats, target.table_status,
                              target.guest_name, target.party_size
                    """,
                    (rs, rowNum) -> new TableView(
                            rs.getInt("table_number"),
                            rs.getInt("seats"),
                            rs.getString("table_status"),
                            rs.getString("guest_name"),
                            rs.getInt("party_size")),
                    request.partySize(), guestName, request.partySize());
        }

        if (seated.isEmpty()) {
            if (request.tableNumber() != null && !tableExists(request.tableNumber())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That table does not exist.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No open table with enough seats is available.");
        }
        return seated.get(0);
    }

    @DeleteMapping("/tables/{tableNumber}/seating")
    public TableView release(@PathVariable int tableNumber) {
        int changed = jdbc.update("""
                UPDATE dining_table
                SET table_status = 'open', guest_name = '', party_size = 0, table_update = now()
                WHERE table_number = ? AND table_status = 'occupied'
                """, tableNumber);
        if (changed == 0) {
            if (!tableExists(tableNumber)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That table does not exist.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That table is already open.");
        }
        return table(tableNumber);
    }

    @GetMapping("/menu")
    public List<MenuItemView> menu() {
        return jdbc.query("""
                SELECT item_id, item_name, category, price
                FROM menu_items
                WHERE is_available
                ORDER BY category, item_name
                """,
                (rs, rowNum) -> new MenuItemView(
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getString("category"),
                        rs.getBigDecimal("price")));
    }

    @GetMapping("/orders")
    public List<OrderView> orders() {
        return jdbc.query("""
                SELECT co.order_id, dt.table_number, co.order_status,
                       co.order_creation,
                       COALESCE(string_agg(mi.item_name || ' x ' || oi.quantity,
                                           ', ' ORDER BY mi.item_name), '') AS items
                FROM customer_order co
                JOIN dining_table dt ON dt.table_id = co.table_id
                LEFT JOIN order_item oi ON oi.order_id = co.order_id
                LEFT JOIN menu_items mi ON mi.item_id = oi.item_id
                GROUP BY co.order_id, dt.table_number
                ORDER BY co.order_creation DESC, co.order_id DESC
                LIMIT 100
                """,
                (rs, rowNum) -> new OrderView(
                        rs.getInt("order_id"),
                        rs.getInt("table_number"),
                        rs.getString("order_status"),
                        rs.getTimestamp("order_creation").toInstant().toString(),
                        rs.getString("items")));
    }

    @PostMapping("/orders")
    @Transactional
    public OrderView addOrder(@RequestBody AddOrderRequest request) {
        if (request == null || request.tableNumber() == null || request.tableNumber() < 1
                || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a table and enter at least one menu item.");
        }

        Integer tableId = jdbc.query("""
                SELECT table_id FROM dining_table
                WHERE table_number = ? AND table_status = 'occupied'
                """, rs -> rs.next() ? rs.getInt(1) : null, request.tableNumber());
        if (tableId == null) {
            if (!tableExists(request.tableNumber())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That table does not exist.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seat a party at this table before adding an order.");
        }

        Integer waiterId = jdbc.query("""
                SELECT ws.waiter_id
                FROM waiter_station ws
                JOIN worker w ON w.worker_id = ws.worker_id
                WHERE w.username = 'jdoe' AND w.is_active
                """, rs -> rs.next() ? rs.getInt(1) : null);
        if (waiterId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The sample waiter is not configured in the database.");
        }

        Map<Integer, Integer> requestedItems = new LinkedHashMap<>();
        for (OrderLine line : request.items()) {
            if (line == null || line.itemId() == null || line.quantity() == null
                    || line.quantity() < 1 || line.quantity() > 50) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each item needs a menu item ID and a quantity from 1 to 50.");
            }
            requestedItems.merge(line.itemId(), line.quantity(), Integer::sum);
        }
        if (requestedItems.values().stream().anyMatch(quantity -> quantity > 50)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The quantity for an item cannot exceed 50.");
        }

        List<Integer> availableIds = jdbc.query("""
                SELECT item_id FROM menu_items WHERE is_available
                """, (rs, rowNum) -> rs.getInt(1));
        if (!availableIds.containsAll(requestedItems.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more selected menu items are unavailable.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO customer_order (table_id, waiter_id)
                    VALUES (?, ?)
                    """, new String[] {"order_id"});
            statement.setInt(1, tableId);
            statement.setInt(2, waiterId);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return the new order ID.");
        }
        int orderId = generatedId.intValue();

        List<Object[]> itemParameters = new ArrayList<>();
        requestedItems.forEach((itemId, quantity) -> itemParameters.add(
                new Object[] {orderId, itemId, quantity}));
        jdbc.batchUpdate("""
                INSERT INTO order_item (order_id, item_id, quantity)
                VALUES (?, ?, ?)
                """, itemParameters);
        return jdbc.queryForObject("""
                SELECT co.order_id, dt.table_number, co.order_status, co.order_creation,
                       string_agg(mi.item_name || ' x ' || oi.quantity,
                                  ', ' ORDER BY mi.item_name) AS items
                FROM customer_order co
                JOIN dining_table dt ON dt.table_id = co.table_id
                JOIN order_item oi ON oi.order_id = co.order_id
                JOIN menu_items mi ON mi.item_id = oi.item_id
                WHERE co.order_id = ?
                GROUP BY co.order_id, dt.table_number
                """,
                (rs, rowNum) -> new OrderView(
                        rs.getInt("order_id"),
                        rs.getInt("table_number"),
                        rs.getString("order_status"),
                        rs.getTimestamp("order_creation").toInstant().toString(),
                        rs.getString("items")),
                orderId);
    }

    private TableView table(int tableNumber) {
        return jdbc.query("""
                SELECT table_number, seats, table_status, guest_name, party_size
                FROM dining_table WHERE table_number = ?
                """,
                rs -> rs.next()
                        ? new TableView(rs.getInt("table_number"), rs.getInt("seats"),
                                rs.getString("table_status"), rs.getString("guest_name"),
                                rs.getInt("party_size"))
                        : null,
                tableNumber);
    }

    private boolean tableExists(int tableNumber) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM dining_table WHERE table_number = ?)",
                Boolean.class, tableNumber);
        return Boolean.TRUE.equals(exists);
    }

    public record AddTableRequest(Integer tableNumber, Integer capacity) {}
    public record SeatRequest(String guestName, Integer partySize, Integer tableNumber) {}
    public record OrderLine(Integer itemId, Integer quantity) {}
    public record AddOrderRequest(Integer tableNumber, List<OrderLine> items) {}
    public record AssignWaiterRequest(Integer tableNumber, Integer waiterId) {}
    public record KitchenStatusRequest(String status) {}
    public record TableView(int tableNumber, int capacity, String status, String guestName, int partySize) {}
    public record MenuItemView(int itemId, String itemName, String category, java.math.BigDecimal price) {}
    public record OrderView(int orderId, int tableNumber, String status, String createdAt, String items) {}
    public record SummaryView(long activeWorkers, long totalTables, long occupiedTables,
            long totalOrders, long openOrders, java.math.BigDecimal collected) {}
    public record HealthView(String database, String checkedAt, long workers,
            long tables, long orders) {}
    public record WorkerView(int workerId, String firstName, String lastName,
            String username, boolean active, List<String> roles) {}
    public record WaiterView(int waiterId, String firstName, String lastName, boolean active) {}
    public record TableAssignmentView(int tableNumber, int waiterId, String firstName, String lastName) {}
    public record KitchenItemView(int orderId, int orderItemId, int tableNumber, String itemName,
            int quantity, String notes, String status, int priority) {}
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleStatus(
            ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request failed." : exception.getReason();
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("error", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleInvalidBody() {
        return org.springframework.http.ResponseEntity.badRequest()
                .body(Map.of("error", "The request body is missing or contains invalid data."));
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleDatabaseUnavailable() {
        return org.springframework.http.ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error",
                        "The database is unavailable. Check that PostgreSQL is running and the app database settings match."));
    }
}
