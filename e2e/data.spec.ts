import { test, expect } from "@playwright/test";

test("authenticated waiter can create and read back a dining table", async ({ page }) => {
  const suffix = Date.now();
  const username = `data${suffix}`;
  const password = "StrongPassword123!";

  await page.goto("/signup.html");
  await page.locator("#signup-name").fill(`Data Waiter ${suffix}`);
  await page.locator("#signup-username").fill(username);
  await page.locator("#signup-password").fill(password);
  await page.locator("#signup-role").selectOption("WAITER");
  await page.locator("#signup-form button[type=submit]").click();
  await page.waitForURL(/\/login\.html\?signup=success/);

  await page.locator("#login-form input[name=username]").fill(username);
  await page.locator("#login-form input[name=password]").fill(password);
  await page.locator("#login-form button[type=submit]").click();
  await page.waitForURL((url) => url.pathname !== "/login.html");

  const tableNumber = 10000 + (suffix % 80000);
  const result = await page.evaluate(async ({ tableNumber }) => {
    const csrfResponse = await fetch("/api/auth/csrf");
    const csrf = await csrfResponse.json();
    const createResponse = await fetch("/api/tables", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-XSRF-TOKEN": csrf.token,
      },
      body: JSON.stringify({ tableNumber, capacity: 4 }),
    });
    const tablesResponse = await fetch("/api/tables");
    return {
      createStatus: createResponse.status,
      createBody: await createResponse.json(),
      readStatus: tablesResponse.status,
      tables: await tablesResponse.json(),
    };
  }, { tableNumber });

  expect(result.createStatus).toBe(200);
  expect(result.readStatus).toBe(200);
  expect(result.tables).toContainEqual(
    expect.objectContaining({ tableNumber, capacity: 4 })
  );
});
