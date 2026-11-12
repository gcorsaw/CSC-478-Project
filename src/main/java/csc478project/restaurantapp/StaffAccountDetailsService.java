package csc478project.restaurantapp;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class StaffAccountDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final UserDetailsService bootstrapAccounts;

    public StaffAccountDetailsService(JdbcTemplate jdbc, UserDetailsService bootstrapAccounts) {
        this.jdbc = jdbc;
        this.bootstrapAccounts = bootstrapAccounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        List<UserDetails> accounts = jdbc.query(
            "SELECT a.username, a.password_hash, w.role "
                + "FROM staff_account a JOIN worker w ON w.worker_id = a.worker_id "
                + "WHERE a.username = ? AND w.is_active = true",
            (resultSet, rowNumber) -> User.withUsername(resultSet.getString("username"))
                .password(resultSet.getString("password_hash"))
                .roles(resultSet.getString("role"))
                .build(),
            normalizedUsername
        );
        return accounts.isEmpty() ? bootstrapAccounts.loadUserByUsername(username) : accounts.get(0);
    }
}