package csc478project.restaurantapp;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class RestaurantApiController {

    private final TerminalSessionService sessionService;

    public RestaurantApiController(TerminalSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/terminal/auth")
    public Map<String, Object> handleAuth(@RequestParam String empId, @RequestParam String pin, HttpSession session) {
        String resolvedRole = sessionService.validateCredentials(empId, pin);
        if (!"GUEST".equals(resolvedRole)) {
            session.setAttribute("UID", empId);
            session.setAttribute("ROLE", resolvedRole);
            return Map.of("success", true, "role", resolvedRole, "empId", empId);
        }
        return Map.of("success", false, "message", "Access Denied.");
    }

    @GetMapping("/terminal/identity")
    public Map<String, Object> getIdentity(HttpSession session) {
        Object uid = session.getAttribute("UID");
        Object role = session.getAttribute("ROLE");
        if (uid != null) {
            return Map.of("authenticated", true, "role", role, "uid", uid);
        }
        return Map.of("authenticated", false, "role", "GUEST");
    }

    @GetMapping("/tables/layout")
    public Collection<RestaurantTable> fetchLayout() {
        return sessionService.getAllTables();
    }

    @PostMapping("/override/raise")
    public Map<String, Object> raiseRequest(@RequestParam int tableId, @RequestParam String description, HttpSession session) {
        Object uid = session.getAttribute("UID");
        if (uid == null) throw new RuntimeException("Unauthenticated terminal session.");
        return sessionService.createOverride(uid.toString(), tableId, description);
    }

    @GetMapping("/override/incoming")
    public List<Map<String, Object>> showManagerQueue(HttpSession session) {
        Object role = session.getAttribute("ROLE");
        if ("MANAGER".equals(role) || "OWNER".equals(role)) {
            return sessionService.getPendingRequests();
        }
        throw new RuntimeException("Clearance level violation.");
    }

    @PostMapping("/override/signoff")
    public Map<String, Object> signoff(@RequestParam int ticketId, @RequestParam String decision, HttpSession session) {
        Object uid = session.getAttribute("UID");
        Object role = session.getAttribute("ROLE");
        if (!"MANAGER".equals(role) && !"OWNER".equals(role)) throw new RuntimeException("Access Denied.");
        boolean state = sessionService.updateOverride(ticketId, decision, uid.toString());
        return Map.of("success", state);
    }

    @GetMapping("/override/poll/{ticketId}")
    public Map<String, Object> trackTicket(@PathVariable int ticketId) {
        return sessionService.checkStatus(ticketId);
    }
    
    @PostMapping("/terminal/clear")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("status", "TERMINAL_LOCKED");
    }
}
