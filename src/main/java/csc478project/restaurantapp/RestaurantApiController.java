package csc478project.restaurantapp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestaurantApiController {
    private final RestaurantService restaurantService;
    private final ManagerOverrideService managerOverrideService;

    public RestaurantApiController(
        RestaurantService restaurantService,
        ManagerOverrideService managerOverrideService
    ) {
        this.restaurantService = restaurantService;
        this.managerOverrideService = managerOverrideService;
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        return restaurantService.getTables();
    }

    @PostMapping("/tables")
    public Map<String, String> addTable(@RequestBody Map<String, Object> request) {
        return restaurantService.addTable(
            toInteger(bodyValue(request, "tableNumber")),
            toInteger(bodyValue(request, "capacity"))
        );
    }

    @PostMapping("/orders")
    public Map<String, Object> processOrder(@RequestBody Map<String, Object> request) {
        return restaurantService.createOrder(
            toInteger(bodyValue(request, "tableNumber")),
            toInteger(bodyValue(request, "waiterId")),
            parseOrderItems(bodyValue(request, "items"))
        );
    }

    @PostMapping("/orders/{orderId}/items/{itemId}/refill")
    public Map<String, String> incrementBeverageRefill(
        @PathVariable int orderId,
        @PathVariable int itemId
    ) {
        restaurantService.incrementBeverageRefill(orderId, itemId);
        return Map.of("message", "Beverage refill incremented successfully.");
    }

    @PostMapping("/manager/override")
    public ResponseEntity<Map<String, Object>> checkManagerOverride(
        @RequestBody Map<String, String> credentials
    ) {
        boolean authorized = managerOverrideService.isAuthorized(
            credentials == null ? null : credentials.get("username"),
            credentials == null ? null : credentials.get("password")
        );
        if (!authorized) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "Manager authentication authorization failed."
            );
        }
        return ResponseEntity.ok(Map.of("authorized", true, "token", UUID.randomUUID().toString()));
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> getAllOrders() {
        return restaurantService.getAllOrders();
    }

    @GetMapping("/summary")
    public Map<String, Object> getOwnerSummary() {
        return restaurantService.getOwnerSummary();
    }

    @GetMapping("/admin/health")
    public Map<String, Object> getAdminHealth() {
        return restaurantService.getAdminHealth();
    }

    @GetMapping("/waiters")
    public List<Map<String, Object>> getWaiters() {
        return restaurantService.getWaiters();
    }

    @GetMapping("/menu")
    public List<Map<String, Object>> getMenu() {
        return restaurantService.getMenu();
    }

    private static List<RestaurantService.OrderItemInput> parseOrderItems(Object rawItems) {
        if (!(rawItems instanceof List<?> items)) {
            return null;
        }

        List<RestaurantService.OrderItemInput> parsedItems = new ArrayList<>();
        for (Object rawItem : items) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                return null;
            }
            parsedItems.add(new RestaurantService.OrderItemInput(
                toInteger(item.get("itemId")),
                toInteger(item.get("quantity"))
            ));
        }
        return parsedItems;
    }

    private static Integer toInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private static Object bodyValue(Map<String, Object> body, String key) {
        return body == null ? null : body.get(key);
    }
}
