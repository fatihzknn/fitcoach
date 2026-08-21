import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test("a trainer can walk My Program -> onboarding -> plan-selection -> today entirely through the UI", async ({
  page,
  context,
  trainerAccount,
}) => {
  // Deliberately does NOT use seedSession() to pre-set fc_onboarded/fc_plan —
  // every other test in this file does, which is exactly why a real bug
  // slipped through here once: the "My Program" link gets prefetched while
  // the trainer is still unonboarded, and Next's client router cache can
  // replay that stale redirect-to-/onboarding after onboarding/plan-selection
  // genuinely finish (fixed by switching the post-onboarding and
  // post-plan-selection redirects to hard navigations). Only a full,
  // real-click walk from a cold session reproduces that class of bug.
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/trainer");

  await page.getByRole("link", { name: "My Program" }).click();
  await expect(page).toHaveURL(/\/onboarding/, { timeout: 10_000 });

  await page.getByRole("button", { name: "Muscle gain" }).click();
  await page.getByRole("button", { name: "I’m just starting" }).click();
  await page.getByRole("button", { name: "3 days" }).click();
  await page.getByRole("button", { name: "60 min" }).click();
  await page.getByLabel("Age").fill("29");
  await page.getByLabel("Height (cm)").fill("180");
  await page.getByLabel("Weight (kg)").fill("82");
  await page.getByRole("button", { name: "Male", exact: true }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button", { name: "I’m comfortable with barbell lifts" }).click();
  await page.getByRole("button", { name: "None", exact: true }).click();
  await page.getByRole("button", { name: "Finish setup" }).click();
  await expect(page).toHaveURL(/\/plan-selection/, { timeout: 15_000 });

  await page.getByRole("button", { name: "Start with this plan" }).click();
  await expect(page).toHaveURL(/\/today/, { timeout: 15_000 });
  await expect(page.getByRole("button", { name: /Start workout/ })).toBeVisible({ timeout: 10_000 });

  // Repeat visits via the panel link must also land correctly, not replay
  // whatever got cached during the very first (pre-onboarding) prefetch.
  await page.getByRole("link", { name: "Trainer panel" }).click();
  await expect(page).toHaveURL(/\/trainer$/, { timeout: 10_000 });
  await page.getByRole("link", { name: "My Program" }).click();
  await expect(page).toHaveURL(/\/today/, { timeout: 10_000 });
});

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
