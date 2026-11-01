package csc478project.restaurantapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock
    private JdbcTemplate jdbc;

    private RestaurantService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantService(jdbc);
    }

    @Test
    void addTableRejectsNonPositiveTableNumberWithoutDatabaseWrites() {
        ApiException exception = assertThrows(ApiException.class, () -> service.addTable(0, 4));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test
    void addTableRejectsDuplicateTableNumber() {
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM dining_table WHERE table_number = ?"),
            eq(Long.class),
            eq(4)
        )).thenReturn(1L);

        ApiException exception = assertThrows(ApiException.class, () -> service.addTable(4, 4));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void addTableMapsConcurrentDuplicateInsertToConflict() {
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM dining_table WHERE table_number = ?"),
            eq(Long.class),
            eq(4)
        )).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate table number"))
            .when(jdbc)
            .update(
                eq("INSERT INTO dining_table (table_number, capacity, table_status) VALUES (?, ?, 'available')"),
                eq(4),
                eq(4)
            );

        ApiException exception = assertThrows(ApiException.class, () -> service.addTable(4, 4));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void createOrderRequiresAtLeastOneItem() {
        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.createOrder(1, 3, List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test
    void createOrderAssignsWaiterAndAddsItemsToNewOrder() {
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM dining_table WHERE table_number = ?"),
            eq(Long.class),
            eq(4)
        )).thenReturn(1L);
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM worker WHERE worker_id = ? AND role = 'WAITER' AND is_active = true"),
            eq(Long.class),
            eq(3)
        )).thenReturn(1L);
        when(jdbc.queryForObject(
            eq("SELECT count(*) FROM menu_items WHERE item_id = ? AND is_available = true"),
            eq(Long.class),
            eq(2)
        )).thenReturn(1L);
        when(jdbc.queryForList(
            eq("SELECT order_id FROM customer_order WHERE table_number = ? AND order_status IN ('open', 'preparing')"),
            eq(Integer.class),
            eq(4)
        )).thenReturn(List.of());
        when(jdbc.queryForObject(
            eq("INSERT INTO customer_order (table_number, waiter_id, order_status) VALUES (?, ?, 'open') RETURNING order_id"),
            eq(Integer.class),
            eq(4),
            eq(3)
        )).thenReturn(42);

        var result = service.createOrder(4, 3, List.of(new RestaurantService.OrderItemInput(2, 2)));

        assertEquals(42, result.get("orderId"));
        verify(jdbc).update(
            eq("UPDATE dining_table SET table_status = 'occupied' WHERE table_number = ?"),
            eq(4)
        );
        verify(jdbc).update(
            eq("INSERT INTO order_item (order_id, item_id, quantity) VALUES (?, ?, ?)"),
            eq(42),
            eq(2),
            eq(2)
        );
    }
}
