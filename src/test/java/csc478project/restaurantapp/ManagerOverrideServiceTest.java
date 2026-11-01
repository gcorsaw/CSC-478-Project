package csc478project.restaurantapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ManagerOverrideServiceTest {
    @Mock
    private JdbcTemplate jdbc;

    @Test
    void authorizesActiveManagerWithConfiguredSecret() {
        ManagerOverrideService service = new ManagerOverrideService(jdbc, "runtime-secret");
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM worker WHERE name = ? AND role = 'MANAGER' AND is_active = true"),
            eq(Long.class),
            eq("Alice Smith")
        )).thenReturn(1L);

        assertTrue(service.isAuthorized("Alice Smith", "runtime-secret"));
    }

    @Test
    void rejectsWrongSecretWithoutLookingUpManager() {
        ManagerOverrideService service = new ManagerOverrideService(jdbc, "runtime-secret");

        assertFalse(service.isAuthorized("Alice Smith", "incorrect"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsInactiveOrUnknownManager() {
        ManagerOverrideService service = new ManagerOverrideService(jdbc, "runtime-secret");
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM worker WHERE name = ? AND role = 'MANAGER' AND is_active = true"),
            eq(Long.class),
            eq("Unknown Manager")
        )).thenReturn(0L);

        assertFalse(service.isAuthorized("Unknown Manager", "runtime-secret"));
    }

    @Test
    void failsClosedWhenSecretIsNotConfigured() {
        ManagerOverrideService service = new ManagerOverrideService(jdbc, "");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.isAuthorized("Alice Smith", "runtime-secret")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        verifyNoInteractions(jdbc);
    }
}
