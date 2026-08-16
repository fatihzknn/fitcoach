import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { registerUser, uniqueEmail } from "./support/api";

test("a trainer visiting a client route is bounced to /trainer", async ({ page, context, trainerAccount }) => {
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/today");
  await expect(page).toHaveURL(/\/trainer$/);
});

test("a fully set-up client visiting /trainer is bounced to /today", async ({ page, context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/trainer");
  await expect(page).toHaveURL(/\/today/);
});

test("an authed but not-yet-onboarded client hitting /today is sent to /onboarding", async ({
  page,
  context,
  request,
}) => {
  const auth = await registerUser(request, {
    email: uniqueEmail("guard-onb"),
    password: "password123",
    displayName: "Guard Onb",
    isTrainer: false,
  });
  await seedSession(context, { token: auth.token, role: "USER" });
  await page.goto("/today");
  await expect(page).toHaveURL(/\/onboarding/);
});

test("an onboarded client with no plan hitting /today is sent to /plan-selection", async ({
  page,
  context,
  request,
}) => {
  const auth = await registerUser(request, {
    email: uniqueEmail("guard-plan"),
    password: "password123",
    displayName: "Guard Plan",
    isTrainer: false,
  });
  await seedSession(context, { token: auth.token, role: "USER", onboarded: true });
  await page.goto("/today");
  await expect(page).toHaveURL(/\/plan-selection/);
});

test("a fully set-up client visiting /login is redirected home", async ({ page, context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/login");
  await expect(page).toHaveURL(/\/today/);
});

test("a trainer visiting /register is redirected to /trainer", async ({ page, context, trainerAccount }) => {
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/register");
  await expect(page).toHaveURL(/\/trainer$/);
});

test("root path routes each session state to the right home", async ({ page, context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/");
  await expect(page).toHaveURL(/\/today/);
});
