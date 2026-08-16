import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test.beforeEach(async ({ context, readyClient, page }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/check-in");
});

function ratingGroup(page: import("@playwright/test").Page, label: string) {
  return page.getByText(label, { exact: true }).locator("..");
}

test("submits a full weekly check-in and can jump to progress", async ({ page }) => {
  await page.getByLabel("Body weight (kg)").fill("77.4");

  await ratingGroup(page, "Sleep quality").getByRole("button", { name: "4", exact: true }).click();
  await ratingGroup(page, "Energy levels").getByRole("button", { name: "3", exact: true }).click();
  await ratingGroup(page, "Stress").getByRole("button", { name: "2", exact: true }).click();

  await page.getByRole("button", { name: "Mild" }).click();
  await page.getByLabel("Notes (optional)").fill("Felt good this week.");

  await page.getByRole("button", { name: "Save check-in" }).click();

  await expect(page.getByText("Check-in saved")).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "View progress" }).click();
  await expect(page).toHaveURL(/\/progress/);
});

test("shows an extra warning for moderate/severe pain without blocking submission", async ({ page }) => {
  await page.getByRole("button", { name: "Severe" }).click();
  await expect(page.getByText(/Never train through severe pain/)).toBeVisible();

  await page.getByRole("button", { name: "Save check-in" }).click();
  await expect(page.getByText("Check-in saved")).toBeVisible({ timeout: 10_000 });
});

test("weight and ratings are optional — an empty submit still saves", async ({ page }) => {
  await page.getByRole("button", { name: "Save check-in" }).click();
  await expect(page.getByText("Check-in saved")).toBeVisible({ timeout: 10_000 });
});
