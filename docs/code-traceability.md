# Code Traceability

This map links the implemented API behavior to its service, database tables,
and automated tests. The design boundary is the controller-to-service
separation: controllers translate HTTP input and output; services validate
business rules and coordinate database work.

| Requirement | API and service | Data | Verification |
|---|---|---|---|
| REQ-TABLE-01: Add a table only when its number and capacity are valid and its number is unique. | `POST /api/tables` → `RestaurantApiController.addTable` → `RestaurantService.addTable` | `dining_table` | `RestaurantServiceTest.addTableRejectsNonPositiveTableNumberWithoutDatabaseWrites`; `RestaurantServiceTest.addTableRejectsDuplicateTableNumber` |
| REQ-ORDER-01: Create an order only for an existing table, an active waiter, and available menu items; attach the selected waiter and all valid items. | `POST /api/orders` → `RestaurantApiController.processOrder` → `RestaurantService.createOrder` | `customer_order`, `dining_table`, `worker`, `menu_items`, `order_item` | `RestaurantServiceTest.createOrderRequiresAtLeastOneItem`; `RestaurantServiceTest.createOrderAssignsWaiterAndAddsItemsToNewOrder` |
| REQ-AUTH-01: Authorize a manager override only when the configured runtime secret matches and the named manager is active. Reject requests when no secret is configured. | `POST /api/manager/override` → `RestaurantApiController.checkManagerOverride` → `ManagerOverrideService.isAuthorized` | `worker` | `ManagerOverrideServiceTest` |
| REQ-AUTH-02: Reject anonymous API requests and enforce role-based access to staff, manager, owner, and admin resources. | `SecurityConfig.securityFilterChain` → Spring Security filter chain | Staff accounts and in-memory/bootstrap identities | `SecurityConfigTest.rejectsAnonymousApiRequests`; `SecurityConfigTest.rejectsStaffRoleFromAdminApi`; `SecurityConfigTest.allowsManagerRolePastDashboardSummaryAuthorization`; `SecurityConfigTest.permitsAdminRolePastAdminApiAuthorization` |
| REQ-AUTH-03: Allow staff self-signup for approved roles and allow managers or owners to create accounts under the applicable role policy. | `POST /api/auth/signup`, `POST /api/staff/accounts` → `AuthController` / `StaffAccountController` → `StaffAccountService` | `staff_account`, `worker` | `ApplicationUnitTest.authControllerCreatesStaffSignupWithCreatedStatus`; `ApplicationUnitTest.staffAccountControllerAllowsOwnerToCreateManagerAccount`; `StaffAccountServiceTest` |
| REQ-TABLE-02: Return the configured dining tables and their status. | `GET /api/tables` → `RestaurantApiController.getTables` → `RestaurantService.getTables` | `dining_table` | `ApplicationUnitTest.restaurantControllerDelegatesReadOnlyDashboardEndpoints`; `e2e/data.spec.ts` |
| REQ-ORDER-02: Update an existing open/preparing order or create a new order and mark its table occupied. | `POST /api/orders` → `RestaurantService.createOrder` | `customer_order`, `dining_table`, `order_item` | `RestaurantServiceTest.createOrderAssignsWaiterAndAddsItemsToNewOrder`; `ApplicationUnitTest.restaurantControllerDelegatesTableAndOrderOperations` |
| REQ-ORDER-03: Increment a beverage refill count only for an existing order-item relationship. | `POST /api/orders/{orderId}/items/{itemId}/refill` → `RestaurantService.incrementBeverageRefill` | `order_item` | `ApplicationUnitTest.restaurantControllerReturnsBeverageRefillConfirmation` |

The manager override secret is runtime configuration, not a source-code
credential. This shared-secret demo mechanism does not replace individual
accounts or full role-based authorization; those remain product requirements.
