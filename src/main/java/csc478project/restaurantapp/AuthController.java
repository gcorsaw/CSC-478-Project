package csc478project.restaurantapp;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final StaffAccountService staffAccountService;

    public AuthController(StaffAccountService staffAccountService) {
        this.staffAccountService = staffAccountService;
    }

    @GetMapping("/api/auth/csrf")
    public Map<String, String> csrfToken(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @GetMapping("/api/auth/session")
    public Map<String, String> currentSession(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
            .replaceFirst("^ROLE_", "");
        return Map.of("username", authentication.getName(), "role", role);
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<Map<String, Object>> signUp(@RequestBody SignupRequest request) {
        Map<String, Object> account = staffAccountService.signUp(
            request.name(), request.username(), request.password(), request.role()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/api/auth/owner-setup-available")
    public Map<String, Boolean> ownerSetupAvailability() {
        return Map.of("available", staffAccountService.isOwnerSetupAvailable());
    }

    @PostMapping("/api/auth/owner-setup")
    public ResponseEntity<Map<String, Object>> createFirstOwner(@RequestBody OwnerSetupRequest request) {
        Map<String, Object> account = staffAccountService.createFirstOwner(
            request.name(), request.username(), request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    public record SignupRequest(String name, String username, String password, String role) {}
    public record OwnerSetupRequest(String name, String username, String password) {}
}