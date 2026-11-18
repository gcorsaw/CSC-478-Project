import { test, expect } from "@playwright/test";

test("public portal renders and protects API data", async ({ page, request }) => {
  await page.goto("/restaurant.html");
  await expect(page).toHaveTitle(/Green Olives/i);
  await expect(page.getByRole("heading", { name: "Welcome to Green Olives" })).toBeVisible();

  const menu = await request.get("/api/menu");
  expect(menu.status()).toBe(401);
});
