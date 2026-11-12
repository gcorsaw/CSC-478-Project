package csc478project.restaurantapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTest {
    private static final String INSERT_ACCOUNT_SQL =
        "INSERT INTO staff_account (worker_id, username, password_hash) VALUES (?, ?, ?)";
    private static final String OWNER_COUNT_SQL =
        "SELECT count(*) FROM staff_account a JOIN worker w ON w.worker_id = a.worker_id WHERE w.role = 'OWNER'";

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StaffAccountsProperties bootstrapAccounts;

    private StaffAccountService service;

    @Test
    void createsEmployeeAndStoresEncodedPassword() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of(
                "worker_id", 42,
                "name", "New Chef",
                "role", "CHEF",
                "is_active", true
            ));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(passwordEncoder.encode("temporary-password-42")).thenReturn("bcrypt-hash");
        when(jdbc.update(INSERT_ACCOUNT_SQL, 42, "new-chef", "bcrypt-hash")).thenReturn(1);

        Map<String, Object> created = service.createAccount(
            "New Chef", "New-Chef", "temporary-password-42", "CHEF", false
        );

        assertEquals(42, created.get("workerId"));
        assertEquals("new-chef", created.get("username"));
        assertEquals("CHEF", created.get("role"));
        verify(passwordEncoder).encode("temporary-password-42");
        verify(jdbc).update(INSERT_ACCOUNT_SQL, 42, "new-chef", "bcrypt-hash");
    }

    @Test
    void managerCannotCreatePrivilegedAccount() {
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);
        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.createAccount(
                "New Manager", "new-manager", "temporary-password-42", "MANAGER", false
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(jdbc, passwordEncoder);
    }

    @Test
    void rejectsDuplicateUsernameAndReportsConflict() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("worker_id", 43));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(passwordEncoder.encode("temporary-password-42")).thenReturn("bcrypt-hash");
        when(jdbc.update(INSERT_ACCOUNT_SQL, 43, "new-chef", "bcrypt-hash"))
            .thenThrow(new DuplicateKeyException("duplicate username"));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.createAccount(
                "New Chef", "new-chef", "temporary-password-42", "CHEF", false
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void allowsSelfSignupWithoutAnInviteForStaffRoles() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of(
                "worker_id", 44,
                "name", "New Host",
                "role", "HOST",
                "is_active", true
            ));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(passwordEncoder.encode("temporary-password-42")).thenReturn("bcrypt-hash");
        when(jdbc.update(INSERT_ACCOUNT_SQL, 44, "new-host", "bcrypt-hash")).thenReturn(1);

        Map<String, Object> created = service.signUp(
            "New Host", "new-host", "temporary-password-42", "HOST"
        );

        assertEquals("new-host", created.get("username"));
        assertEquals("HOST", created.get("role"));
    }

    @Test
    void rejectsPrivilegedRolesFromPublicSignup() {
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.signUp(
                "New Manager", "new-manager", "temporary-password-42", "MANAGER"
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(jdbc, passwordEncoder);
    }

    @Test
    void enablesOwnerSetupWhenNoOwnerExists() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        when(jdbc.queryForObject(OWNER_COUNT_SQL, Long.class)).thenReturn(0L);
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);

        assertTrue(service.isOwnerSetupAvailable());
    }

    @Test
    void createsFirstOwnerWithoutAnAdditionalSetupCode() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        when(jdbc.queryForObject(OWNER_COUNT_SQL, Long.class)).thenReturn(0L);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of(
                "worker_id", 45,
                "name", "First Owner",
                "role", "OWNER",
                "is_active", true
            ));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(passwordEncoder.encode("temporary-owner-password-42")).thenReturn("bcrypt-hash");
        when(jdbc.update(INSERT_ACCOUNT_SQL, 45, "owner", "bcrypt-hash")).thenReturn(1);
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);

        Map<String, Object> created = service.createFirstOwner(
            "First Owner", "Owner", "temporary-owner-password-42"
        );

        assertEquals("owner", created.get("username"));
        assertEquals("OWNER", created.get("role"));
        verify(jdbc).execute("SELECT pg_advisory_xact_lock(478001)");
    }

    @Test
    void rejectsOwnerSetupAfterAnOwnerAlreadyExists() {
        when(bootstrapAccounts.getAccounts()).thenReturn(Map.of());
        when(jdbc.queryForObject(OWNER_COUNT_SQL, Long.class)).thenReturn(1L);
        service = new StaffAccountService(jdbc, passwordEncoder, bootstrapAccounts);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.createFirstOwner(
                "Second Owner", "second-owner", "temporary-owner-password-42"
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verifyNoInteractions(passwordEncoder);
    }
}