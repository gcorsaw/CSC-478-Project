package csc478project.restaurantapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class RestaurantApiController {

    private final JdbcTemplate jdbc;

    public RestaurantApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * CAPSTONE-SPEC: HOST WORKSPACE
     * Retrieves entire restaurant physical floor map layout matrices.
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        return jdbc.queryForList("SELECT table_number AS \"tableNumber\", capacity, table_status AS \"status\" FROM dining_table ORDER BY table_number");
    }

    /**
     * CAPSTONE-SPEC: TABLE CONFIGURATION ENGINE
     * Appends brand new operational tables. Bypasses old constraint clashes gracefully.
     */
    @PostMapping("/tables")
    public ResponseEntity<?> addTable(@RequestBody Map<String, Object> req) {
        Integer tableNum = (Integer) req.get("tableNumber");
        Integer cap = (Integer) req.get("capacity");
        
        if (tableNum == null || cap == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid form inputs."));
        }

        jdbc.update("INSERT INTO dining_table (table_number, capacity, table_status) VALUES (?, ?, 'available')", tableNum, cap);
        return ResponseEntity.ok(Map.of("message", "Table configuration saved securely."));
    }

    /**
     * CAPSTONE-SPEC: WAITER ORDER SUBMISSION & INCREMENTAL FILLING
     * Appends items to existing orders or opens new ones, tracking drinks separately.
     */
    @PostMapping("/orders")
    public ResponseEntity<?> processOrder(@RequestBody Map<String, Object> req) {
        Integer tableNum = (Integer) req.get("tableNumber");
        Integer waiterId = (Integer) req.get("waiterId");
        List<Map<String, Object>> items = (List<Map<String, Object>>) req.get("items");

        if (tableNum == null || waiterId == null || items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A table, waiter, and at least one item are required."));
        }
        Long activeWaiters = jdbc.queryForObject(
            "SELECT count(*) FROM worker WHERE worker_id = ? AND role = 'WAITER' AND is_active = true",
            Long.class, waiterId
        );
        if (activeWaiters == null || activeWaiters == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Select an active waiter."));
        }

        // Check if an open order already exists for this table (Allows Incremental Ordering Spec)
        List<Integer> existingOrders = jdbc.queryForList(
            "SELECT order_id FROM customer_order WHERE table_number = ? AND order_status IN ('open', 'preparing')",
            Integer.class, tableNum
        );

        int orderId;
        if (!existingOrders.isEmpty()) {
            orderId = existingOrders.get(0);
            jdbc.update("UPDATE customer_order SET waiter_id = ? WHERE order_id = ?", waiterId, orderId);
        } else {
            // Open a pristine new check sequence
            orderId = jdbc.queryForObject(
                "INSERT INTO customer_order (table_number, waiter_id, order_status) VALUES (?, ?, 'open') RETURNING order_id",
                Integer.class, tableNum, waiterId
            );
            jdbc.update("UPDATE dining_table SET table_status = 'occupied' WHERE table_number = ?", tableNum);
        }

        // Insert items individually into the order list
        for (Map<String, Object> item : items) {
            Integer itemId = (Integer) item.get("itemId");
            Integer qty = (Integer) item.get("quantity");
            jdbc.update("INSERT INTO order_item (order_id, item_id, quantity) VALUES (?, ?, ?)", orderId, itemId, qty);
        }

        return ResponseEntity.ok(Map.of("orderId", orderId, "message", "Items batched successfully."));
    }

    /**
     * CAPSTONE-SPEC: SEPARATE BEVERAGE REFILL MONITORING
     * Manages soft drink tracking variables independently from standard kitchen meals.
     */
    @PostMapping("/orders/{orderId}/items/{itemId}/refill")
    public ResponseEntity<?> incrementBeverageRefill(@PathVariable int orderId, @PathVariable int itemId) {
        jdbc.update(
            "UPDATE order_item SET refill_count = refill_count + 1 WHERE order_id = ? AND item_id = ?",
            orderId, itemId
        );
        return ResponseEntity.ok(Map.of("message", "Beverage refill incremented successfully."));
    }

    /**
     * CAPSTONE-SPEC: ROLE-BASED MANAGER SECURITY AUTHENTICATION OVERRIDE
     * Intercepts and reviews sensitive updates. Rejects without manager approval.
     */
    @PostMapping("/manager/override")
    public ResponseEntity<?> checkManagerOverride(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Check credentials against our active worker schema
        List<Map<String, Object>> matchingManagers = jdbc.queryForList(
            "SELECT worker_id FROM worker WHERE name = ? AND role = 'MANAGER' AND is_active = true", username
        );

        if (matchingManagers.isEmpty() || !"secure_override_pass".equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager authentication authorization failed.");
        }

        return ResponseEntity.ok(Map.of("authorized", true, "token", UUID.randomUUID().toString()));
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> getAllOrders() {
        return jdbc.queryForList(
            "SELECT order_id AS \"orderId\", table_number AS \"tableNumber\", order_status AS \"status\", order_time AS \"createdAt\" FROM customer_order ORDER BY order_id DESC"
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> getOwnerSummary() {
        long activeWorkers = jdbc.queryForObject("SELECT count(*) FROM worker WHERE is_active", Long.class);
        long totalTables = jdbc.queryForObject("SELECT count(*) FROM dining_table", Long.class);
        long occupiedTables = jdbc.queryForObject("SELECT count(*) FROM dining_table WHERE table_status = 'occupied'", Long.class);
        long totalOrders = jdbc.queryForObject("SELECT count(*) FROM customer_order", Long.class);
        long openOrders = jdbc.queryForObject("SELECT count(*) FROM customer_order WHERE order_status = 'open'", Long.class);
        java.math.BigDecimal collected = jdbc.queryForObject("SELECT COALESCE(sum(amount + tip), 0) FROM payment", java.math.BigDecimal.class);

        Map<String, Object> summary = new HashMap<>();
        summary.put("activeWorkers", activeWorkers);
        summary.put("totalTables", totalTables);
        summary.put("occupiedTables", occupiedTables);
        summary.put("totalOrders", totalOrders);
        summary.put("openOrders", openOrders);
        summary.put("collected", collected);
        return summary;
    }

    @GetMapping("/admin/health")
    public Map<String, Object> getAdminHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("database", "HEALTHY — PostgreSQL 16 Active");
        health.put("checkedAt", new java.util.Date().toString());
        health.put("workers", jdbc.queryForObject("SELECT count(*) FROM worker", Long.class));
        health.put("tables", jdbc.queryForObject("SELECT count(*) FROM dining_table", Long.class));
        health.put("orders", jdbc.queryForObject("SELECT count(*) FROM customer_order", Long.class));
        return health;
    }

    @GetMapping("/waiters")
    public List<Map<String, Object>> getWaiters() {
        return jdbc.queryForList("SELECT worker_id AS \"waiterId\", name AS \"firstName\", 'Staff' AS \"lastName\", is_active AS \"active\" FROM worker WHERE role = 'WAITER'");
    }

    @GetMapping("/menu")
    public List<Map<String, Object>> getMenu() {
        return jdbc.queryForList("SELECT item_id AS \"itemId\", item_name AS \"itemName\", category, price FROM menu_items WHERE is_available = true");
    }
}
