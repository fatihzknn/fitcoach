import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test("the panel's My Program link leads a fresh trainer into onboarding", async ({
  page,
  context,
  trainerAccount,
}) => {
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/trainer");
  await page.getByRole("link", { name: "My Program" }).click();
  await expect(page).toHaveURL(/\/onboarding/);
});

test("a self-tracking trainer reaches /today directly and sees their program", async ({
  page,
  context,
  readyTrainer,
}) => {
  await seedSession(context, { token: readyTrainer.token, role: "TRAINER", onboarded: true, planSelected: true });
  await page.goto("/today");
  await expect(page.getByRole("heading", { name: new RegExp(readyTrainer.displayName) })).toBeVisible({
    timeout: 10_000,
  });
});

test("the Messages tab is hidden from a self-tracking trainer's nav", async ({ page, context, readyTrainer }) => {
  await seedSession(context, { token: readyTrainer.token, role: "TRAINER", onboarded: true, planSelected: true });
  await page.goto("/today");
  await expect(page.getByRole("button", { name: /Start workout/ })).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("link", { name: "Messages" })).not.toBeVisible();
});

test("the header's Trainer panel link returns a trainer to /trainer", async ({ page, context, readyTrainer }) => {
  await seedSession(context, { token: readyTrainer.token, role: "TRAINER", onboarded: true, planSelected: true });
  await page.goto("/today");
  await page.getByRole("link", { name: "Trainer panel" }).click();
  await expect(page).toHaveURL(/\/trainer$/);
});

test("a self-tracking trainer visiting /messages directly is bounced to /today", async ({
  page,
  context,
  readyTrainer,
}) => {
  await seedSession(context, { token: readyTrainer.token, role: "TRAINER", onboarded: true, planSelected: true });
  await page.goto("/messages");
  await expect(page).toHaveURL(/\/today/);
});

test("a plain panel-only trainer's dashboard is unaffected", async ({ page, context, trainerAccount }) => {
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/trainer");
  await expect(page.getByText("Your invite code", { exact: true })).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("No clients yet")).toBeVisible();
});
