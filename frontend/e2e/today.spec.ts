import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test.beforeEach(async ({ context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
});

test("greets the client by name and shows today's workout", async ({ page, readyClient }) => {
  await page.goto("/today");
  await expect(page.getByRole("heading", { name: new RegExp(readyClient.displayName) })).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.getByRole("button", { name: /Start workout/ })).toBeVisible();
});

test("switching the day tab changes the displayed workout", async ({ page }) => {
  await page.goto("/today");
  await expect(page.getByRole("button", { name: /Start workout/ })).toBeVisible({ timeout: 10_000 });

  const dayTwo = page.getByRole("button", { name: "Day 2" });
  if (await dayTwo.count()) {
    const initialCard = await page.locator("main, section").first().innerText();
    await dayTwo.click();
    await expect(async () => {
      const after = await page.locator("main, section").first().innerText();
      expect(after).not.toBe(initialCard);
    }).toPass({ timeout: 5_000 });
  }
});

test("starting a workout navigates to the session screen", async ({ page }) => {
  await page.goto("/today");
  await page.getByRole("button", { name: /Start workout/ }).click();
  await expect(page).toHaveURL(/\/workout\/.+/, { timeout: 10_000 });
});
