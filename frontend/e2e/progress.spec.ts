import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { submitCheckIn, saveMeasurement } from "./support/api";

test.beforeEach(async ({ context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
});

test("shows empty states before any check-in or measurement exists", async ({ page }) => {
  await page.goto("/progress");
  await expect(page.getByText("No check-ins yet")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("button", { name: "Log first measurement" })).toBeVisible();
});

test("reflects a submitted check-in and measurement", async ({ page, request, readyClient }) => {
  await submitCheckIn(request, readyClient.token, { weightKg: 79, sleepQualityRating: 4 });
  await saveMeasurement(request, readyClient.token, { neckCm: 38, waistCm: 84 });

  await page.goto("/progress");
  await expect(page.getByText(/Athlete|Fitness|Average|Essential fat|Above avg/)).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("Check-in history")).toBeVisible();
});

test("Measure and Check in header buttons navigate correctly", async ({ page }) => {
  await page.goto("/progress");
  await page.getByRole("link", { name: /Measure/ }).click();
  await expect(page).toHaveURL(/\/measurements/);

  await page.goBack();
  await page.getByRole("link", { name: /Check in/ }).click();
  await expect(page).toHaveURL(/\/check-in/);
});
