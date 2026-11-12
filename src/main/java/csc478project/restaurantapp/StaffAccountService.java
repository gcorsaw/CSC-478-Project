package csc478project.restaurantapp;

import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountService {
    private static final Set<String> STAFF_ROLES = Set.of(
        "MANAGER", "OWNER", "ADMIN", "CHEF", "WAITER", "HOST"
    );
    private static final Set<String> MANAGER_ASSIGNABLE_ROLES = Set.of("CHEF", "WAITER", "HOST");
    private static final Set<String> SELF_SIGNUP_ROLES = Set.of("CHEF", "WAITER", "HOST");
    private static final long OWNER_SETUP_LOCK_KEY = 478001L;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final StaffAccountsProperties bootstrapAccounts;

    public StaffAccountService(
        JdbcTemplate jdbc,
        PasswordEncoder passwordEncoder,
        StaffAccountsProperties bootstrapAccounts
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAccounts = bootstrapAccounts;
    }

    public boolean isOwnerSetupAvailable() {
        return !hasBootstrapOwner() && !hasDatabaseOwnerAccount();
    }

    @Transactional
    public Map<String, Object> createFirstOwner(
        String name,
        String username,
        String password
    ) {
        jdbc.execute("SELECT pg_advisory_xact_lock(" + OWNER_SETUP_LOCK_KEY + ")");
        if (hasBootstrapOwner() || hasDatabaseOwnerAccount()) {
            throw new ApiException(HttpStatus.CONFLICT, "An Owner account has already been created.");
        }
        return createAccount(name, username, password, "OWNER", true);
    }

    @Transactional
    public Map<String, Object> signUp(
        String name,
        String username,
        String password,
        String role
    ) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!SELF_SIGNUP_ROLES.contains(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a role available for staff sign up.");
        }
        return createAccount(name, username, password, normalizedRole, false);
    }

    @Transactional
    public Map<String, Object> createAccount(
        String name,
        String username,
        String password,
        String role,
        boolean owner
    ) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedUsername = username == null
            ? ""
            : username.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Employee name is required and must be 100 characters or fewer.");
        }
        if (!normalizedUsername.matches("[a-z0-9._-]{3,32}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username must be 3 to 32 characters using letters, numbers, dots, underscores, or hyphens.");
        }
        if (password == null || password.length() < 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters.");
        }
        if (!STAFF_ROLES.contains(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid staff role.");
        }
        if (!owner && !MANAGER_ASSIGNABLE_ROLES.contains(normalizedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only an owner can create manager, admin, or owner accounts.");
        }
        if (bootstrapAccounts.getAccounts().values().stream()
            .filter(account -> account != null && account.getPassword() != null && !account.getPassword().isBlank())
            .filter(account -> account.getUsername() != null)
            .anyMatch(account -> normalizedUsername.equalsIgnoreCase(account.getUsername().trim()))) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is reserved for a configured account.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO worker (name, role, is_active) VALUES (?, ?, true)",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, normalizedName);
            statement.setString(2, normalizedRole);
            return statement;
        }, keyHolder);

        Map<String, Object> generatedKeys = keyHolder.getKeys();
        Object generatedWorkerId = generatedKeys == null ? null : generatedKeys.get("worker_id");
        if (!(generatedWorkerId instanceof Number workerId)) {
            throw new IllegalStateException("The database did not return the new employee ID.");
        }

        try {
            jdbc.update(
                "INSERT INTO staff_account (worker_id, username, password_hash) VALUES (?, ?, ?)",
                workerId.intValue(), normalizedUsername, passwordEncoder.encode(password)
            );
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is already in use.");
        }

        return Map.of(
            "workerId", workerId.intValue(),
            "name", normalizedName,
            "username", normalizedUsername,
            "role", normalizedRole
        );
    }

    private boolean hasBootstrapOwner() {
        return bootstrapAccounts.getAccounts().entrySet().stream()
            .filter(entry -> "OWNER".equalsIgnoreCase(entry.getKey()))
            .map(Map.Entry::getValue)
            .anyMatch(account -> account != null
                && account.getUsername() != null && !account.getUsername().isBlank()
                && account.getPassword() != null && !account.getPassword().isBlank());
    }

    private boolean hasDatabaseOwnerAccount() {
        Long ownerAccounts = jdbc.queryForObject(
            "SELECT count(*) FROM staff_account a JOIN worker w ON w.worker_id = a.worker_id WHERE w.role = 'OWNER'",
            Long.class
        );
        return ownerAccounts != null && ownerAccounts > 0;
    }
}