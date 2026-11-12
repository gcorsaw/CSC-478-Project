package csc478project.restaurantapp;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/accounts")
public class StaffAccountController {
    private final StaffAccountService staffAccountService;

    public StaffAccountController(StaffAccountService staffAccountService) {
        this.staffAccountService = staffAccountService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAccount(
        @RequestBody StaffAccountRequest request,
        Authentication authentication
    ) {
        boolean owner = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"));
        Map<String, Object> account = staffAccountService.createAccount(
            request.name(), request.username(), request.password(), request.role(), owner
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    public record StaffAccountRequest(String name, String username, String password, String role) {}
}