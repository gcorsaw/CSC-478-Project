package csc478project.restaurantapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@ExtendWith(MockitoExtension.class)
class StaffAccountDetailsServiceTest {
    private static final String LOOKUP_SQL =
        "SELECT a.username, a.password_hash, w.role "
            + "FROM staff_account a JOIN worker w ON w.worker_id = a.worker_id "
            + "WHERE a.username = ? AND w.is_active = true";

    @Mock
    private JdbcTemplate jdbc;

    @Test
    void loadsPersistentAccountsWithTrimmedCaseInsensitiveUsername() {
        UserDetails storedAccount = User.withUsername("new-chef")
            .password("bcrypt-hash")
            .roles("CHEF")
            .build();
        when(jdbc.query(eq(LOOKUP_SQL), anyRowMapper(), eq("new-chef")))
            .thenReturn(List.of(storedAccount));
        StaffAccountDetailsService service = new StaffAccountDetailsService(jdbc, username -> {
            throw new AssertionError("Database account should be used before bootstrap accounts.");
        });

        UserDetails loaded = service.loadUserByUsername(" New-Chef ");

        assertEquals("bcrypt-hash", loaded.getPassword());
        assertEquals("ROLE_CHEF", loaded.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void fallsBackToConfiguredBootstrapAccounts() {
        UserDetails bootstrapAccount = User.withUsername("manager")
            .password("{noop}test-password")
            .roles("MANAGER")
            .build();
        var bootstrap = new InMemoryUserDetailsManager(bootstrapAccount);
        when(jdbc.query(eq(LOOKUP_SQL), anyRowMapper(), eq("manager"))).thenReturn(List.of());
        StaffAccountDetailsService service = new StaffAccountDetailsService(jdbc, bootstrap);

        assertEquals("ROLE_MANAGER", service.loadUserByUsername("manager")
            .getAuthorities().iterator().next().getAuthority());
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<UserDetails> anyRowMapper() {
        return (RowMapper<UserDetails>) any(RowMapper.class);
    }
}