package csc478project.restaurantapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ManagerOverrideService {
    private final JdbcTemplate jdbc;
    private final String configuredOverridePassword;

    public ManagerOverrideService(
        JdbcTemplate jdbc,
        @Value("${restaurant.manager.override-password:}") String configuredOverridePassword
    ) {
        this.jdbc = jdbc;
        this.configuredOverridePassword = configuredOverridePassword;
    }

    public boolean isAuthorized(String username, String password) {
        if (configuredOverridePassword.isBlank()) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Manager override is not configured. Set RESTAURANT_MANAGER_OVERRIDE_PASSWORD."
            );
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        boolean validSecret = MessageDigest.isEqual(
            configuredOverridePassword.getBytes(StandardCharsets.UTF_8),
            password.getBytes(StandardCharsets.UTF_8)
        );
        if (!validSecret) {
            return false;
        }

        Long activeManagers = jdbc.queryForObject(
            "SELECT count(*) FROM worker WHERE name = ? AND role = 'MANAGER' AND is_active = true",
            Long.class, username
        );
        return activeManagers != null && activeManagers > 0;
    }
}
