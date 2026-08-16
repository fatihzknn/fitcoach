import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { getInviteCode, redeemInviteCode } from "./support/api";

async function gotoLinkedClientDetail(
  page: import("@playwright/test").Page,
  context: import("@playwright/test").BrowserContext,
  request: import("@playwright/test").APIRequestContext,
  trainerAccount: { token: string },
  readyClient: { token: string; displayName: string },
) {
  const { code } = await getInviteCode(request, trainerAccount.token);
  await redeemInviteCode(request, readyClient.token, code);
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });

  await page.goto("/trainer");
  await page.getByText(readyClient.displayName).click();
  await expect(page).toHaveURL(/\/trainer\/clients\/.+/);
}

test("trainer can (re)assign a plan to a client", async ({ page, context, request, trainerAccount, readyClient }) => {
  await gotoLinkedClientDetail(page, context, request, trainerAccount, readyClient);

  await page.getByRole("button", { name: /Assign a plan|Change plan/ }).click();
  await expect(page.getByRole("button", { name: "Start with this plan" })).toBeVisible({ timeout: 10_000 });

  await page.getByRole("button", { name: "Start with this plan" }).click();
  await expect(page.getByText("Plan assigned.")).toBeVisible({ timeout: 10_000 });
});

test("trainer can edit a client's fitness profile", async ({ page, context, request, trainerAccount, readyClient }) => {
  await gotoLinkedClientDetail(page, context, request, trainerAccount, readyClient);

  await page.getByRole("button", { name: "Edit profile" }).click();
  await expect(page).toHaveURL(/\/trainer\/clients\/.+\/profile/);

  await page.getByRole("button", { name: "Fat loss" }).click();
  await page.getByRole("button", { name: "Save changes" }).click();

  await expect(page).toHaveURL(/\/trainer\/clients\/[^/]+$/, { timeout: 10_000 });
});

test("trainer can build and save a custom plan for a client", async ({
  page,
  context,
  request,
  trainerAccount,
  readyClient,
}) => {
  await gotoLinkedClientDetail(page, context, request, trainerAccount, readyClient);

  await page.getByRole("button", { name: "Build custom plan" }).click();
  await expect(page).toHaveURL(/\/trainer\/clients\/.+\/custom-plan/);

  await page.getByLabel("Plan name").fill("E2E Strength Block");
  await page.getByRole("button", { name: "Get stronger" }).click();
  await page.getByPlaceholder("Day 1 name").fill("Day 1 — Push");

  await page.getByRole("button", { name: "Add exercise" }).click();
  await expect(page.getByPlaceholder("Search exercises...")).toBeVisible();
  await page.getByPlaceholder("Search exercises...").fill("Bench");
  // First button in the sheet is the unlabeled close (X) icon; the first
  // filtered result is the next one.
  await page.locator(".fixed.inset-0.z-50 button").nth(1).click();

  await page.getByRole("button", { name: "Save plan" }).click();
  await expect(page).toHaveURL(/\/trainer\/clients\/[^/]+$/, { timeout: 10_000 });
});
