import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { buildReadyClient, DEFAULT_ONBOARDING } from "./support/api";

test.beforeEach(async ({ context, readyClient, page }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/measurements");
});

test("male client sees neck+waist as required and gets a US Navy BF% result", async ({ page }) => {
  await expect(page.getByText("US Navy circumference method")).toBeVisible();
  await page.getByLabel("Body weight").fill("80");
  await page.getByLabel("Neck").fill("38");
  await page.getByLabel("Waist / abdomen").fill("85");

  await page.getByRole("button", { name: "Save measurements" }).click();

  await expect(page.getByText("Measurements saved")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("Estimated body fat")).toBeVisible();
  await expect(page.getByText("US Navy method")).toBeVisible();
});

test("Log another resets the form without navigating away", async ({ page }) => {
  await page.getByLabel("Neck").fill("38");
  await page.getByLabel("Waist / abdomen").fill("85");
  await page.getByRole("button", { name: "Save measurements" }).click();
  await expect(page.getByText("Measurements saved")).toBeVisible({ timeout: 10_000 });

  await page.getByRole("button", { name: "Log another" }).click();
  await expect(page.getByRole("button", { name: "Save measurements" })).toBeVisible();
  await expect(page).toHaveURL(/\/measurements/);
});

test("female client sees hip as the required field for BAI", async ({ page, context, request }) => {
  const female = await buildReadyClient(request, {
    onboarding: { ...DEFAULT_ONBOARDING, sex: "FEMALE" },
  });
  await seedSession(context, { token: female.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/measurements");

  await expect(page.getByText("Body Adiposity Index")).toBeVisible();
  await page.getByLabel("Hip circumference").fill("95");
  await page.getByRole("button", { name: "Save measurements" }).click();

  await expect(page.getByText("Measurements saved")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("Body Adiposity Index")).toBeVisible();
});
