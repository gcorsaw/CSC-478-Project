package csc478project.restaurantapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ApplicationUnitTest {
    @Mock
    private RestaurantService restaurantService;

    @Mock
    private ManagerOverrideService managerOverrideService;

    @Mock
    private StaffAccountService staffAccountService;

    @Mock
    private Authentication authentication;

    @Test
    void restaurantControllerDelegatesTableAndOrderOperations() {
        RestaurantApiController controller =
            new RestaurantApiController(restaurantService, managerOverrideService);

        when(restaurantService.addTable(4, 6))
            .thenReturn(Map.of("message", "Table configured"));
        when(restaurantService.createOrder(
            eq(4),
            eq(3),
            eq(List.of(new RestaurantService.OrderItemInput(2, 2)))
        )).thenReturn(Map.of("orderId", 42));

        assertEquals(
            "Table configured",
            controller.addTable(Map.of("tableNumber", 4, "capacity", 6)).get("message")
        );
        assertEquals(
            42,
            controller.processOrder(Map.of(
                "tableNumber", 4,
                "waiterId", 3,
                "items", List.of(Map.of("itemId", 2, "quantity", 2))
            )).get("orderId")
        );

        verify(restaurantService).addTable(4, 6);
        verify(restaurantService).createOrder(
            4,
            3,
            List.of(new RestaurantService.OrderItemInput(2, 2))
        );
    }

    @Test
    void restaurantControllerPassesMalformedOrderItemsToValidation() {
        RestaurantApiController controller =
            new RestaurantApiController(restaurantService, managerOverrideService);

        controller.processOrder(Map.of(
            "tableNumber", 4,
            "waiterId", 3,
            "items", List.of("not-a-menu-item")
        ));

        verify(restaurantService).createOrder(4, 3, null);
    }

    @Test
    void restaurantControllerReturnsBeverageRefillConfirmation() {
        RestaurantApiController controller =
            new RestaurantApiController(restaurantService, managerOverrideService);

        Map<String, String> response = controller.incrementBeverageRefill(12, 7);

        assertEquals("Beverage refill incremented successfully.", response.get("message"));
        verify(restaurantService).incrementBeverageRefill(12, 7);
    }

    @Test
    void restaurantControllerRejectsFailedManagerOverride() {
        RestaurantApiController controller =
            new RestaurantApiController(restaurantService, managerOverrideService);
        when(managerOverrideService.isAuthorized("Alice", "wrong"))
            .thenReturn(false);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> controller.checkManagerOverride(Map.of(
                "username", "Alice",
                "password", "wrong"
            ))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void restaurantControllerDelegatesReadOnlyDashboardEndpoints() {
        RestaurantApiController controller =
            new RestaurantApiController(restaurantService, managerOverrideService);
        when(restaurantService.getTables()).thenReturn(List.of(Map.of("tableNumber", 4)));
        when(restaurantService.getAllOrders()).thenReturn(List.of(Map.of("orderId", 12)));
        when(restaurantService.getWaiters()).thenReturn(List.of(Map.of("waiterId", 3)));
        when(restaurantService.getMenu()).thenReturn(List.of(Map.of("itemId", 2)));

        assertEquals(1, controller.getTables().size());
        assertEquals(1, controller.getAllOrders().size());
        assertEquals(1, controller.getWaiters().size());
        assertEquals(1, controller.getMenu().size());
    }

    @Test
    void authControllerReturnsCurrentSessionAndOwnerSetupStatus() {
        AuthController controller = new AuthController(staffAccountService);
        when(authentication.getAuthorities())
            .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        when(authentication.getName()).thenReturn("owner");
        when(staffAccountService.isOwnerSetupAvailable()).thenReturn(false);

        assertEquals(
            Map.of("username", "owner", "role", "OWNER"),
            controller.currentSession(authentication)
        );
        assertEquals(
            Map.of("available", false),
            controller.ownerSetupAvailability()
        );
    }

    @Test
    void staffAccountControllerAllowsOwnerToCreateManagerAccount() {
        StaffAccountController controller = new StaffAccountController(staffAccountService);
        when(authentication.getAuthorities())
            .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        when(staffAccountService.createAccount(
            "Jane Manager",
            "jane-manager",
            "a-long-password",
            "MANAGER",
            true
        )).thenReturn(Map.of("username", "jane-manager", "role", "MANAGER"));

        var response = controller.createAccount(
            new StaffAccountController.StaffAccountRequest(
                "Jane Manager",
                "jane-manager",
                "a-long-password",
                "MANAGER"
            ),
            authentication
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("MANAGER", response.getBody().get("role"));
    }

    @Test
    void authControllerCreatesStaffSignupWithCreatedStatus() {
        AuthController controller = new AuthController(staffAccountService);
        when(staffAccountService.signUp(
            "Jamie Chef",
            "jamie-chef",
            "a-long-password",
            "CHEF"
        )).thenReturn(Map.of("username", "jamie-chef", "role", "CHEF"));

        var response = controller.signUp(new AuthController.SignupRequest(
            "Jamie Chef",
            "jamie-chef",
            "a-long-password",
            "CHEF"
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("jamie-chef", response.getBody().get("username"));
    }
}
