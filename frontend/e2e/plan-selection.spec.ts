import { test, expect } from "./support/fixtures";
import { registerUser, completeOnboarding, getTrainers, uniqueEmail } from "./support/api";
import { seedSession } from "./support/session";

async function registerOnboardedClient(request: import("@playwright/test").APIRequestContext) {
  const auth = await registerUser(request, {
    email: uniqueEmail("plan-selection"),
    password: "password123",
    displayName: "Plan Picker",
    isTrainer: false,
  });
  await completeOnboarding(request, auth.token);
  return auth;
}

test("recommends a plan, shows an alternative, and starting the recommended one lands on today", async ({
  page,
  context,
  request,
}) => {
  const auth = await registerOnboardedClient(request);
  await seedSession(context, { token: auth.token, role: "USER", onboarded: true });

  await page.goto("/plan-selection");
  await expect(page.getByText("Recommended for you")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("Choose this instead")).toBeVisible();

  await page.getByRole("button", { name: "Start with this plan" }).click();
  await expect(page).toHaveURL(/\/today/, { timeout: 15_000 });
});

test("can pick the alternative plan instead of the recommended one", async ({ page, context, request }) => {
  const auth = await registerOnboardedClient(request);
  await seedSession(context, { token: auth.token, role: "USER", onboarded: true });

  await page.goto("/plan-selection");
  await expect(page.getByRole("button", { name: "Choose this instead" })).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "Choose this instead" }).click();
  await expect(page).toHaveURL(/\/today/, { timeout: 15_000 });
});

test("switching training philosophy reloads plan options", async ({ page, context, request }) => {
  const auth = await registerOnboardedClient(request);
  const trainers = await getTrainers(request, auth.token);
  await seedSession(context, { token: auth.token, role: "USER", onboarded: true });

  await page.goto("/plan-selection");
  await expect(page.getByText("Recommended for you")).toBeVisible({ timeout: 10_000 });

  const second = trainers[1];
  if (second) {
    await page.getByRole("button", { name: String(second["displayName"]) }).click();
    await expect(page.getByText("Recommended for you")).toBeVisible({ timeout: 10_000 });
  }
});
