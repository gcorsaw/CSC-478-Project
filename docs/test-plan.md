# Test Plan and Results

## Scope

The automated checks cover service validation, HTTP/API authorization,
controller delegation, staff authentication, and critical browser flows.

## Unit and Integration Tests

Run:

```bash
mvn test
```

| Test area | Input / setup | Expected result | Rational basis | Result |
|---|---|---|---|---|
| Table validation | Table number `0` | HTTP-style `BAD_REQUEST`; no JDBC writes | Prevent invalid table records | Passed |
| Duplicate table | Existing table number `4` | `CONFLICT` | Preserve unique table numbers | Passed |
| Concurrent duplicate table | Database throws duplicate-key error | `CONFLICT` | Map race-condition failure cleanly | Passed |
| Empty order | Table, waiter, and empty item list | `BAD_REQUEST`; no JDBC writes | An order requires at least one item | Passed |
| Order creation | Existing table `4`, waiter `3`, available item `2` | Order `42` created and item inserted | Core order workflow | Passed |
| Manager override | Wrong secret or inactive/ineligible manager | `UNAUTHORIZED` / rejection | Prevent shared-secret misuse | Passed |
| Anonymous API access | Anonymous `GET /api/orders` | `401 Unauthorized` | Protect application data | Passed |
| Role authorization | Manager requests admin API | `403 Forbidden` | Enforce least privilege | Passed |
| Staff signup | Valid CHEF signup data | Account created with `201` | Support approved self-service roles | Passed |
| Staff account creation | Manager/owner account request | Role policy enforced | Prevent privilege escalation | Passed |

## Database Test

Run against Docker:

```bash
./test_database.sh
```

The script initializes a temporary database, applies the schema twice, and
removes the temporary database. Result: use the command output as the final
pass/fail record in the submission.

## Browser Tests

Run with the Docker application available at `http://127.0.0.1:8080`:

```bash
npx playwright test
```

| Browser test | Input / setup | Expected result | Rational basis | Result |
|---|---|---|---|---|
| Public portal | Open `/restaurant.html`, request `/api/menu` anonymously | Page renders; menu API returns `401` | Public view must not expose protected data | Passed |
| Authentication | Invalid login, then valid temporary WAITER signup/login | Invalid login rejected; session restored | Verify login and signup flow | Passed |
| Data workflow | Sign in as temporary WAITER and create/read table `10000-89999` | Table created and returned | Verify browser-to-API-to-database flow | Passed |
