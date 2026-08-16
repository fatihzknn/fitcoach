import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test.beforeEach(async ({ context, readyClient, page }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/today");
  await page.getByRole("button", { name: /Start workout/ }).click();
  await expect(page).toHaveURL(/\/workout\/.+/, { timeout: 10_000 });
});

test("logs a set and the progress counter increments", async ({ page }) => {
  await expect(page.getByText(/0\/\d+ sets logged/)).toBeVisible({ timeout: 10_000 });

  // Each SetRow renders its kg input, reps input, and Log button as siblings in
  // that order with nothing else matching those placeholders/label — index 0
  // across all three always lands on the very first set of the first exercise.
  await page.getByPlaceholder("kg").first().fill("60");
  await page.getByPlaceholder("reps").first().fill("10");
  await page.getByRole("button", { name: "Log", exact: true }).first().click();

  await expect(page.getByText(/1\/\d+ sets logged/)).toBeVisible({ timeout: 10_000 });
});

test("swaps an exercise for a machine-occupied reason and shows the swapped badge", async ({ page }) => {
  await page.getByRole("button", { name: "Swap" }).first().click();
  await expect(page.getByText(/Why can't you do/)).toBeVisible();

  await page.getByRole("button", { name: "Machine is occupied" }).click();
  await expect(page.getByText("Pick a replacement")).toBeVisible();

  // Sheet contains an unlabeled back-arrow button followed by one button per
  // alternative exercise — pick the first alternative.
  const sheet = page.locator(".fixed.inset-0.z-50");
  await sheet.locator("button").nth(1).click();

  await expect(page.getByText("swapped")).toBeVisible({ timeout: 10_000 });
});

test("shows the pain safety notice before offering alternatives for a pain-related swap", async ({ page }) => {
  await page.getByRole("button", { name: "Swap" }).first().click();
  await page.getByRole("button", { name: "Causes pain or discomfort" }).click();

  await expect(page.getByText("Pain safety notice")).toBeVisible();
  await expect(page.getByText(/Never push through pain/)).toBeVisible();

  await page.getByRole("button", { name: "Show alternatives" }).click();
  await expect(page.getByText("Pick a replacement")).toBeVisible();
});

test("can finish a workout early and land back on today", async ({ page }) => {
  await expect(page.getByRole("button", { name: /Finish early/ })).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: /Finish early/ }).click();

  await expect(page.getByText("Workout complete")).toBeVisible({ timeout: 10_000 });
  // The header's back arrow shares the same accessible name ("Back to today")
  // as the completion overlay's CTA — scope to the overlay.
  const overlay = page.locator(".fixed.inset-0").filter({ hasText: "Workout complete" });
  await overlay.getByRole("button", { name: "Back to today" }).click();
  await expect(page).toHaveURL(/\/today/, { timeout: 10_000 });
});
