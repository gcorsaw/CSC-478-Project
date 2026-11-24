# Known Bugs and Issues

## Resolved before submission

- **Port mismatch:** Playwright previously targeted port `8081` while Docker
  Compose published the application on port `8080`. The Playwright base URL now
  matches the documented Compose URL.
- **Compose metadata:** The obsolete Compose `version` field and an outdated
  port comment were removed from `docker-compose.yaml`.

## Current limitations

- The manager override is a shared-secret compatibility mechanism, not an
  individual session token or full audit log.
- Anyone able to reach the application before first-Owner setup can claim
  first-Owner access. The deployment must remain private until setup completes.
- Staff self-signup is intentionally limited to CHEF, WAITER, and HOST, but it
  is open to anyone who can reach the public sign-up page.
- The owner summary's "collected" value depends on payment rows and should not
  be interpreted as a financial ledger for production use.
