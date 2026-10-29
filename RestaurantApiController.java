package csc478project.restaurantapp;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows local front-end testing if running separately
public class RestaurantApiController {

    // Dummy tracking storage mapping to your database entities
    private final List<Map<String, Object>> activeOrders = new ArrayList<>();
    private final List<Map<String, Object>> tableStatus = new ArrayList<>();

    @GetMapping("/orders")
    public List<Map<String, Object>> getOrders() {
        return activeOrders;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> order) {
        order.put("id", UUID.randomUUID().toString());
        order.put("status", "PENDING");
        activeOrders.add(order);
        return order;
    }

    @PutMapping("/orders/{id}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        for (Map<String, Object> order : activeOrders) {
            if (order.get("id").equals(id)) {
                order.put("status", body.get("status"));
                return order;
            }
        }
        throw new RuntimeException("Order not found");
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        return tableStatus;
    }

    @PutMapping("/tables/{tableNumber}")
    public Map<String, Object> updateTable(@PathVariable int tableNumber, @RequestBody Map<String, Object> status) {
        for (Map<String, Object> table : tableStatus) {
            if (table.get("tableNumber").equals(tableNumber)) {
                table.putAll(status);
                return table;
            }
        }
        status.put("tableNumber", tableNumber);
        tableStatus.add(status);
        return status;
    }
}
