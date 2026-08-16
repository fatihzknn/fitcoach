import { test, expect, formAlert } from "./support/fixtures";
import { registerUser, uniqueEmail } from "./support/api";
import { seedSession } from "./support/session";

test("walks a fresh client through all 7 onboarding steps into plan-selection", async ({ page, context, request }) => {
  const auth = await registerUser(request, {
    email: uniqueEmail("onboarding"),
    password: "password123",
    displayName: "Onboarding Walker",
    isTrainer: false,
  });
  await seedSession(context, { token: auth.token, role: "USER" });

  await page.goto("/onboarding");
  await expect(page.getByText("1/7")).toBeVisible();

  // Step 0 — main goal
  await page.getByRole("button", { name: "Muscle gain" }).click();
  await expect(page.getByText("2/7")).toBeVisible();

  // Step 1 — training background
  await page.getByRole("button", { name: "I’m just starting" }).click();
  await expect(page.getByText("3/7")).toBeVisible();

  // Step 2 — days per week
  await page.getByRole("button", { name: "3 days" }).click();
  await expect(page.getByText("4/7")).toBeVisible();

  // Step 3 — session length
  await page.getByRole("button", { name: "60 min" }).click();
  await expect(page.getByText("5/7")).toBeVisible();

  // Step 4 — basics (explicit Continue + validation)
  await page.getByRole("button", { name: "Continue" }).click();
  await expect(formAlert(page)).toBeVisible();

  await page.getByLabel("Age").fill("28");
  await page.getByLabel("Height (cm)").fill("178");
  await page.getByLabel("Weight (kg)").fill("78");
  await page.getByRole("button", { name: "Male", exact: true }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  await expect(page.getByText("6/7")).toBeVisible();

  // Step 5 — barbell comfort
  await page.getByRole("button", { name: "I’m comfortable with barbell lifts" }).click();
  await expect(page.getByText("7/7")).toBeVisible();

  // Step 6 — pain areas, then finish
  await page.getByRole("button", { name: "None", exact: true }).click();
  await page.getByRole("button", { name: "Finish setup" }).click();

  await expect(page).toHaveURL(/\/plan-selection/, { timeout: 15_000 });
});

test("blocks continuing past the basics step with an out-of-range age", async ({ page, context, request }) => {
  const auth = await registerUser(request, {
    email: uniqueEmail("onboarding-invalid"),
    password: "password123",
    displayName: "Invalid Basics",
    isTrainer: false,
  });
  await seedSession(context, { token: auth.token, role: "USER" });

  await page.goto("/onboarding");
  await page.getByRole("button", { name: "Get stronger" }).click();
  await page.getByRole("button", { name: "I’m returning" }).click();
  await page.getByRole("button", { name: "4 days" }).click();
  await page.getByRole("button", { name: "45 min" }).click();

  await page.getByLabel("Age").fill("9");
  await page.getByLabel("Height (cm)").fill("178");
  await page.getByLabel("Weight (kg)").fill("78");
  await page.getByRole("button", { name: "Male", exact: true }).click();
  await page.getByRole("button", { name: "Continue" }).click();

  await expect(formAlert(page)).toHaveText(/valid age/);
  await expect(page.getByText("5/7")).toBeVisible();
});
