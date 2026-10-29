package csc478project.restaurantapp;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TerminalSessionService {

    private final Map<String, String> masterRoster = new ConcurrentHashMap<>();
    private final Map<Integer, RestaurantTable> tableMap = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> liveOverrides = new CopyOnWriteArrayList<>();
    private final AtomicInteger ticketIdCounter = new AtomicInteger(7000);

    public TerminalSessionService() {
        masterRoster.put("EMP-HOST01", "HOST");
        masterRoster.put("EMP-WAIT01", "WAITER");
        masterRoster.put("EMP-CHEF01", "CHEF");
        masterRoster.put("EMP-MGR01", "MANAGER");
        masterRoster.put("EMP-OWNER01", "OWNER");

        for (int i = 1; i <= 8; i++) {
            tableMap.put(i, new RestaurantTable(i, 4));
        }
    }

    public String validateCredentials(String empId, String pin) {
        return masterRoster.getOrDefault(empId, "GUEST");
    }

    public Map<String, Object> createOverride(String staffId, int tableId, String reason) {
        Map<String, Object> req = new ConcurrentHashMap<>();
        req.put("ticketId", ticketIdCounter.getAndIncrement());
        req.put("staffId", staffId);
        req.put("tableId", tableId);
        req.put("details", reason);
        req.put("status", "PENDING_APPROVAL");
        liveOverrides.add(req);
        return req;
    }

    public List<Map<String, Object>> getPendingRequests() {
        return liveOverrides.stream().filter(r -> "PENDING_APPROVAL".equals(r.get("status"))).toList();
    }

    public boolean updateOverride(int ticketId, String action, String managerId) {
        for (Map<String, Object> req : liveOverrides) {
            if ((int) req.get("ticketId") == ticketId) {
                req.put("status", "APPROVE".equalsIgnoreCase(action) ? "APPROVED" : "DENIED");
                req.put("resolver", managerId);
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> checkStatus(int ticketId) {
        return liveOverrides.stream()
                .filter(r -> (int) r.get("ticketId") == ticketId)
                .findFirst()
                .orElse(Map.of("status", "NOT_FOUND"));
    }

    public Collection<RestaurantTable> getAllTables() { return tableMap.values(); }
    public RestaurantTable getTable(int id) { return tableMap.get(id); }
}
