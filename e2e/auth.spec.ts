import { test, expect } from "@playwright/test";

test("invalid sign-in fails and valid account sign-in restores a session", async ({ page }) => {
  await page.goto("/login.html");
  await page.locator("#login-form input[name=username]").fill("not-a-real-user");
  await page.locator("#login-form input[name=password]").fill("incorrect-password");
  await page.locator("#login-form button[type=submit]").click();
  await expect(page.locator("#login-feedback")).toContainText("The username or password was not accepted.");

  const suffix = Date.now();
  const username = `waiter${suffix}`;
  const password = "StrongPassword123!";

  await page.goto("/signup.html");
  await page.locator("#signup-name").fill(`Validation Waiter ${suffix}`);
  await page.locator("#signup-username").fill(username);
  await page.locator("#signup-password").fill(password);
  await page.locator("#signup-role").selectOption("WAITER");
  await page.locator("#signup-form button[type=submit]").click();
  await page.waitForURL(/\/login\.html\?signup=success/);

  await page.locator("#login-form input[name=username]").fill(username);
  await page.locator("#login-form input[name=password]").fill(password);
  await page.locator("#login-form button[type=submit]").click();
  await page.waitForURL((url) => url.pathname !== "/login.html");

  const session = await page.evaluate(() => fetch("/api/auth/session").then((response) => response.json()));
  expect(session.username).toBe(username);
  expect(session.role).toBe("WAITER");
});
