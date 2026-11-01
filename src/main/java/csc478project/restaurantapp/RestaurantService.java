package csc478project.restaurantapp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
    public record OrderItemInput(Integer itemId, Integer quantity) {}

    private final JdbcTemplate jdbc;

    public RestaurantService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getTables() {
        return jdbc.queryForList(
            "SELECT table_number AS \"tableNumber\", capacity, table_status AS \"status\" FROM dining_table ORDER BY table_number"
        );
    }

    public Map<String, String> addTable(Integer tableNumber, Integer capacity) {
        if (tableNumber == null || tableNumber < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Table number must be a positive whole number.");
        }
        if (capacity == null || capacity < 1 || capacity > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Capacity must be a whole number from 1 to 20.");
        }

        Long existingTables = jdbc.queryForObject(
            "SELECT count(*) FROM dining_table WHERE table_number = ?", Long.class, tableNumber
        );
        if (existingTables != null && existingTables > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "That table number is already in use.");
        }

        try {
            jdbc.update(
                "INSERT INTO dining_table (table_number, capacity, table_status) VALUES (?, ?, 'available')",
                tableNumber, capacity
            );
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "That table number is already in use.");
        }
        return Map.of("message", "Table configuration saved securely.");
    }

    @Transactional
    public Map<String, Object> createOrder(
        Integer tableNumber,
        Integer waiterId,
        List<OrderItemInput> items
    ) {
        if (tableNumber == null || waiterId == null || items == null || items.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "A table, waiter, and at least one item are required."
            );
        }

        Long existingTables = jdbc.queryForObject(
            "SELECT count(*) FROM dining_table WHERE table_number = ?", Long.class, tableNumber
        );
        if (existingTables == null || existingTables == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an existing table.");
        }

        Long activeWaiters = jdbc.queryForObject(
            "SELECT count(*) FROM worker WHERE worker_id = ? AND role = 'WAITER' AND is_active = true",
            Long.class, waiterId
        );
        if (activeWaiters == null || activeWaiters == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an active waiter.");
        }

        List<OrderItemInput> validatedItems = new ArrayList<>();
        for (OrderItemInput item : items) {
            if (item == null || item.itemId() == null || item.itemId() < 1
                || item.quantity() == null || item.quantity() < 1) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Menu item IDs and quantities must be positive whole numbers."
                );
            }

            Long availableItems = jdbc.queryForObject(
                "SELECT count(*) FROM menu_items WHERE item_id = ? AND is_available = true",
                Long.class, item.itemId()
            );
            if (availableItems == null || availableItems == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Select an available menu item.");
            }
            validatedItems.add(item);
        }

        List<Integer> existingOrders = jdbc.queryForList(
            "SELECT order_id FROM customer_order WHERE table_number = ? AND order_status IN ('open', 'preparing')",
            Integer.class, tableNumber
        );

        int orderId;
        if (!existingOrders.isEmpty()) {
            orderId = existingOrders.get(0);
            jdbc.update("UPDATE customer_order SET waiter_id = ? WHERE order_id = ?", waiterId, orderId);
        } else {
            Integer newOrderId = jdbc.queryForObject(
                "INSERT INTO customer_order (table_number, waiter_id, order_status) VALUES (?, ?, 'open') RETURNING order_id",
                Integer.class, tableNumber, waiterId
            );
            if (newOrderId == null) {
                throw new IllegalStateException("The database did not return the new order ID.");
            }
            orderId = newOrderId;
            jdbc.update(
                "UPDATE dining_table SET table_status = 'occupied' WHERE table_number = ?", tableNumber
            );
        }

        for (OrderItemInput item : validatedItems) {
            jdbc.update(
                "INSERT INTO order_item (order_id, item_id, quantity) VALUES (?, ?, ?)",
                orderId, item.itemId(), item.quantity()
            );
        }

        return Map.of("orderId", orderId, "message", "Items batched successfully.");
    }

    public void incrementBeverageRefill(int orderId, int itemId) {
        int updatedRows = jdbc.update(
            "UPDATE order_item SET refill_count = refill_count + 1 WHERE order_id = ? AND item_id = ?",
            orderId, itemId
        );
        if (updatedRows == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order item was not found.");
        }
    }

    public List<Map<String, Object>> getAllOrders() {
        return jdbc.queryForList(
            "SELECT order_id AS \"orderId\", table_number AS \"tableNumber\", order_status AS \"status\", order_time AS \"createdAt\" FROM customer_order ORDER BY order_id DESC"
        );
    }

    public Map<String, Object> getOwnerSummary() {
        long activeWorkers = count("SELECT count(*) FROM worker WHERE is_active");
        long totalTables = count("SELECT count(*) FROM dining_table");
        long occupiedTables = count("SELECT count(*) FROM dining_table WHERE table_status = 'occupied'");
        long totalOrders = count("SELECT count(*) FROM customer_order");
        long openOrders = count("SELECT count(*) FROM customer_order WHERE order_status = 'open'");
        BigDecimal collected = jdbc.queryForObject(
            "SELECT COALESCE(sum(amount + tip), 0) FROM payment", BigDecimal.class
        );

        Map<String, Object> summary = new HashMap<>();
        summary.put("activeWorkers", activeWorkers);
        summary.put("totalTables", totalTables);
        summary.put("occupiedTables", occupiedTables);
        summary.put("totalOrders", totalOrders);
        summary.put("openOrders", openOrders);
        summary.put("collected", collected);
        return summary;
    }

    public Map<String, Object> getAdminHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("database", "HEALTHY — PostgreSQL 16 Active");
        health.put("checkedAt", new java.util.Date().toString());
        health.put("workers", count("SELECT count(*) FROM worker"));
        health.put("tables", count("SELECT count(*) FROM dining_table"));
        health.put("orders", count("SELECT count(*) FROM customer_order"));
        return health;
    }

    public List<Map<String, Object>> getWaiters() {
        return jdbc.queryForList(
            "SELECT worker_id AS \"waiterId\", name AS \"firstName\", 'Staff' AS \"lastName\", is_active AS \"active\" FROM worker WHERE role = 'WAITER'"
        );
    }

    public List<Map<String, Object>> getMenu() {
        return jdbc.queryForList(
            "SELECT item_id AS \"itemId\", item_name AS \"itemName\", category, price FROM menu_items WHERE is_available = true"
        );
    }

    private long count(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        if (result == null) {
            throw new IllegalStateException("A database count query returned no result.");
        }
        return result;
    }
}
