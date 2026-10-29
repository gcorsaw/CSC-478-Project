// File: src/main/java/csc478project/restaurantapp/RestaurantApiController.java
package csc478project.restaurantapp;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RestaurantApiController {

    private final List<Map<String, Object>> temporaryOrders = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/orders")
    public List<Map<String, Object>> getOrders() {
        return temporaryOrders;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrdersProcessed", temporaryOrders.size());
        summary.put("dailyRevenue", 0.0);
        summary.put("topSellingItems", new ArrayList<>());
        return summary;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> order) {
        order.put("id", UUID.randomUUID().toString());
        order.put("status", "PENDING");
        temporaryOrders.add(order);
        return order;
    }
}
